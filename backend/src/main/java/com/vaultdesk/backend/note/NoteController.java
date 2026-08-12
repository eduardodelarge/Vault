package com.vaultdesk.backend.note;

import com.vaultdesk.backend.note.dto.NoteCreateRequest;
import com.vaultdesk.backend.note.dto.NoteResponse;
import com.vaultdesk.backend.note.dto.NoteSummaryResponse;
import com.vaultdesk.backend.note.dto.NoteUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    public Page<NoteSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Ordenacao e sempre updated_at DESC (fixa na query nativa do repository); nao aceitamos
        // um "sort" vindo do cliente para essa rota, ver NoteRepository.search().
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return noteService.search(q, pageable).map(NoteSummaryResponse::from);
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteCreateRequest request) {
        Note note = noteService.create(request.title(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.from(note));
    }

    @GetMapping("/{id}")
    public NoteResponse get(@PathVariable UUID id) {
        return NoteResponse.from(noteService.getOwned(id));
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable UUID id, @Valid @RequestBody NoteUpdateRequest request) {
        return NoteResponse.from(noteService.update(id, request.title(), request.content()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        noteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
