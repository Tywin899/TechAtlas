package com.techatlas.dto;

import com.techatlas.entity.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateDocumentRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @NotBlank(message = "Content is required")
    String content,

    @NotBlank(message = "URL is required")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @URL(message = "URL must be a valid format")
    String url,

    @NotNull(message = "Source is required")
    SourceType source,

    @Size(max = 100, message = "Category must not exceed 100 characters")
    String category,

    @Size(max = 255, message = "Author must not exceed 255 characters")
    String author,

    @Size(max = 50, message = "Language must not exceed 50 characters")
    String language,

    String metadata
) {}
