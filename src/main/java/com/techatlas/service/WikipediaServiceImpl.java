package com.techatlas.service;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.WikipediaDiscoverRequest;
import com.techatlas.dto.WikipediaDiscoverResponse;
import com.techatlas.dto.WikipediaSyncStatusResponse;
import com.techatlas.entity.WikipediaSyncArticle;
import com.techatlas.entity.WikipediaSyncCategory;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.fetcher.wikipedia.WikipediaClient;
import com.techatlas.fetcher.wikipedia.dto.WikipediaCategoryResponse;
import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.techatlas.mapper.WikipediaMapper;
import com.techatlas.repository.WikipediaSyncArticleRepository;
import com.techatlas.repository.WikipediaSyncCategoryRepository;
import com.techatlas.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WikipediaServiceImpl implements WikipediaService {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaServiceImpl.class);

    private final WikipediaClient wikipediaClient;
    private final WikipediaMapper wikipediaMapper;
    private final DocumentService documentService;
    private final WikipediaSyncCategoryRepository wikipediaSyncCategoryRepository;
    private final WikipediaSyncArticleRepository wikipediaSyncArticleRepository;
    private final SourceSyncService sourceSyncService;

    public WikipediaServiceImpl(
            WikipediaClient wikipediaClient,
            WikipediaMapper wikipediaMapper,
            DocumentService documentService,
            WikipediaSyncCategoryRepository wikipediaSyncCategoryRepository,
            WikipediaSyncArticleRepository wikipediaSyncArticleRepository,
            SourceSyncService sourceSyncService) {
        this.wikipediaClient = wikipediaClient;
        this.wikipediaMapper = wikipediaMapper;
        this.documentService = documentService;
        this.wikipediaSyncCategoryRepository = wikipediaSyncCategoryRepository;
        this.wikipediaSyncArticleRepository = wikipediaSyncArticleRepository;
        this.sourceSyncService = sourceSyncService;
    }

    @Override
    public WikipediaPageSummary fetchSummary(String title) {
        return wikipediaClient.fetchPageSummary(title);
    }

    @Override
    @Transactional
    public DocumentResponse importArticle(String title) {
        return importArticle(title, null);
    }

    @Override
    @Transactional
    public DocumentResponse importArticle(String title, String category) {
        WikipediaPageSummary summary = wikipediaClient.fetchPageSummary(title);

        String contentHash = HashUtil.calculateSha256(summary.extract());
        Optional<DocumentResponse> existing = documentService.findByContentHash(contentHash);
        if (existing.isPresent()) {
            throw new DuplicateDocumentException("Wikipedia article already imported: " + title);
        }

        CreateDocumentRequest createRequest = wikipediaMapper.toCreateRequest(summary, category);
        DocumentResponse imported = documentService.create(createRequest);

        String extId = summary.pageId() != null ? summary.pageId().toString() : title;
        sourceSyncService.createOrUpdateSyncRecord(
                com.techatlas.entity.SourceType.WIKIPEDIA,
                extId,
                summary.revision(),
                contentHash,
                imported.id()
        );

        return imported;
    }

    private record CategoryDepthPair(String categoryName, int depth) {}

    @Override
    @Transactional
    public WikipediaDiscoverResponse discoverArticles(WikipediaDiscoverRequest request) {
        String startingCategory = request.category().trim();
        int maxArticles = request.maxArticles();
        int maxDepth = request.maxDepth();

        Queue<CategoryDepthPair> queue = new LinkedList<>();
        queue.add(new CategoryDepthPair(startingCategory, 0));

        Set<String> visitedCategoriesInCurrentRun = new HashSet<>();
        
        int articlesDiscovered = 0;
        int articlesImported = 0;
        int duplicatesSkipped = 0;
        int categoriesVisited = 0;

        while (!queue.isEmpty() && articlesImported < maxArticles) {
            CategoryDepthPair current = queue.poll();
            String currentCategoryName = current.categoryName();
            int currentDepth = current.depth();

            String normalizedCategoryKey = currentCategoryName.toLowerCase().replace(" ", "_");
            if (visitedCategoriesInCurrentRun.contains(normalizedCategoryKey)) {
                continue;
            }
            visitedCategoriesInCurrentRun.add(normalizedCategoryKey);

            categoriesVisited++;
            
            // Sync status to database for this category
            WikipediaSyncCategory syncCat = wikipediaSyncCategoryRepository
                    .findByCategoryName(currentCategoryName)
                    .orElse(new WikipediaSyncCategory());
            syncCat.setCategoryName(currentCategoryName);
            syncCat.setLastSyncedAt(LocalDateTime.now());
            wikipediaSyncCategoryRepository.save(syncCat);

            String cmcontinue = null;
            boolean hasNextPage = true;

            while (hasNextPage && articlesImported < maxArticles) {
                WikipediaCategoryResponse response;
                try {
                    response = wikipediaClient.fetchCategoryMembers(currentCategoryName, cmcontinue);
                } catch (Exception e) {
                    logger.error("Failed to fetch category members for [{}]: {}", currentCategoryName, e.getMessage());
                    break;
                }

                if (response == null || response.query() == null || response.query().categorymembers() == null) {
                    break;
                }

                List<WikipediaCategoryResponse.WikipediaCategoryMember> members = response.query().categorymembers();
                for (WikipediaCategoryResponse.WikipediaCategoryMember member : members) {
                    if (member.ns() == 0) {
                        articlesDiscovered++;
                        String articleTitle = member.title();
                        
                        boolean alreadyProcessed = wikipediaSyncArticleRepository.existsByArticleTitle(articleTitle);
                        if (alreadyProcessed) {
                            duplicatesSkipped++;
                            continue;
                        }

                        try {
                            DocumentResponse imported = importArticle(articleTitle, currentCategoryName);
                            articlesImported++;
                            
                            WikipediaSyncArticle syncArt = new WikipediaSyncArticle(articleTitle, LocalDateTime.now(), imported.id());
                            wikipediaSyncArticleRepository.save(syncArt);
                        } catch (DuplicateDocumentException e) {
                            duplicatesSkipped++;
                            WikipediaSyncArticle syncArt = new WikipediaSyncArticle(articleTitle, LocalDateTime.now(), null);
                            wikipediaSyncArticleRepository.save(syncArt);
                        } catch (Exception e) {
                            logger.error("Error importing article [{}]: {}", articleTitle, e.getMessage());
                        }

                        if (articlesImported >= maxArticles) {
                            break;
                        }
                    } else if (member.ns() == 14 && currentDepth < maxDepth) {
                        String subCategoryTitle = member.title();
                        if (subCategoryTitle.startsWith("Category:")) {
                            subCategoryTitle = subCategoryTitle.substring(9);
                        }
                        queue.add(new CategoryDepthPair(subCategoryTitle, currentDepth + 1));
                    }
                }

                if (response.continueToken() != null && response.continueToken().cmcontinue() != null) {
                    cmcontinue = response.continueToken().cmcontinue();
                } else {
                    hasNextPage = false;
                }
            }
        }

        return new WikipediaDiscoverResponse(
                startingCategory,
                articlesDiscovered,
                articlesImported,
                duplicatesSkipped,
                categoriesVisited
        );
    }

    @Override
    public WikipediaSyncStatusResponse getSyncStatus() {
        long totalCategories = wikipediaSyncCategoryRepository.count();
        long totalArticles = wikipediaSyncArticleRepository.count();
        List<WikipediaSyncStatusResponse.CategorySyncInfo> categories = wikipediaSyncCategoryRepository.findAll().stream()
                .map(c -> new WikipediaSyncStatusResponse.CategorySyncInfo(c.getCategoryName(), c.getLastSyncedAt()))
                .toList();
        return new WikipediaSyncStatusResponse(totalCategories, totalArticles, categories);
    }
}
