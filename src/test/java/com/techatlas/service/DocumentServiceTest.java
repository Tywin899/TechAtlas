package com.techatlas.service;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.Document;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import com.techatlas.exception.DocumentNotFoundException;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.mapper.DocumentMapper;
import com.techatlas.repository.DocumentRepository;
import com.techatlas.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    private final DocumentMapper documentMapper = new DocumentMapper();

    private DocumentService documentService;

    private CreateDocumentRequest createRequest;
    private UpdateDocumentRequest updateRequest;
    private Document document;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(documentRepository, documentMapper);

        createRequest = new CreateDocumentRequest(
                "Doc Title",
                "Some unique content",
                "https://example.com/doc",
                SourceType.GITHUB,
                "DevOps",
                "Alice",
                "en",
                "{\"key\":\"value\"}"
        );

        updateRequest = new UpdateDocumentRequest(
                "Doc Title Updated",
                "Some unique content updated",
                "https://example.com/doc",
                SourceType.GITHUB,
                "DevOps",
                "Alice",
                "en",
                DocumentStatus.ACTIVE,
                "{\"key\":\"value\"}"
        );

        document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle(createRequest.title());
        document.setContent(createRequest.content());
        document.setUrl(createRequest.url());
        document.setSource(createRequest.source());
        document.setCategory(createRequest.category());
        document.setAuthor(createRequest.author());
        document.setLanguage(createRequest.language());
        document.setContentHash(HashUtil.calculateSha256(createRequest.content()));
        document.setStatus(DocumentStatus.PENDING_INDEX);
        document.setMetadata(createRequest.metadata());
    }

    @Test
    void testCreateSuccess() {
        String expectedHash = HashUtil.calculateSha256(createRequest.content());
        when(documentRepository.existsByContentHash(expectedHash)).thenReturn(false);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        DocumentResponse response = documentService.create(createRequest);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(createRequest.title(), response.title());
        assertEquals(expectedHash, response.contentHash());
        assertEquals(DocumentStatus.PENDING_INDEX, response.status());
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void testCreateDuplicateHashThrowsException() {
        String expectedHash = HashUtil.calculateSha256(createRequest.content());
        when(documentRepository.existsByContentHash(expectedHash)).thenReturn(true);

        assertThrows(DuplicateDocumentException.class, () -> documentService.create(createRequest));
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void testRetrieveSuccess() {
        UUID id = document.getId();
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));

        DocumentResponse response = documentService.retrieve(id);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(document.getTitle(), response.title());
    }

    @Test
    void testRetrieveNotFoundThrowsException() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentService.retrieve(id));
    }

    @Test
    void testUpdateSuccess() {
        UUID id = document.getId();
        String expectedHash = HashUtil.calculateSha256(updateRequest.content());
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentRepository.existsByContentHash(expectedHash)).thenReturn(false);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.update(id, updateRequest);

        assertNotNull(response);
        assertEquals(updateRequest.title(), response.title());
        assertEquals(updateRequest.content(), response.content());
        assertEquals(expectedHash, response.contentHash());
        assertEquals(DocumentStatus.ACTIVE, response.status());
    }

    @Test
    void testUpdateNotFoundThrowsException() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentService.update(id, updateRequest));
    }

    @Test
    void testUpdateDuplicateHashThrowsException() {
        UUID id = document.getId();
        String expectedHash = HashUtil.calculateSha256(updateRequest.content());
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentRepository.existsByContentHash(expectedHash)).thenReturn(true);

        assertThrows(DuplicateDocumentException.class, () -> documentService.update(id, updateRequest));
    }

    @Test
    void testDeleteSuccess() {
        UUID id = document.getId();
        when(documentRepository.existsById(id)).thenReturn(true);
        doNothing().when(documentRepository).deleteById(id);

        assertDoesNotThrow(() -> documentService.delete(id));
        verify(documentRepository, times(1)).deleteById(id);
    }

    @Test
    void testDeleteNotFoundThrowsException() {
        UUID id = UUID.randomUUID();
        when(documentRepository.existsById(id)).thenReturn(false);

        assertThrows(DocumentNotFoundException.class, () -> documentService.delete(id));
        verify(documentRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void testListAllSuccess() {
        when(documentRepository.findAll()).thenReturn(List.of(document));

        List<DocumentResponse> list = documentService.listAll();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(document.getId(), list.get(0).id());
    }
}
