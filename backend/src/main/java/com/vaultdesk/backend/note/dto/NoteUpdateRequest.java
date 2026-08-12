package com.vaultdesk.backend.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content) {
}
