package com.vaultdesk.backend.note.dto;

import com.vaultdesk.backend.note.Note;
import java.time.Instant;
import java.util.UUID;

public record NoteSummaryResponse(UUID id, String title, Instant createdAt, Instant updatedAt) {

    public static NoteSummaryResponse from(Note note) {
        return new NoteSummaryResponse(note.getId(), note.getTitle(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
