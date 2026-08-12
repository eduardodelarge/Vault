package com.vaultdesk.backend.note;

import com.vaultdesk.backend.common.exception.ResourceNotFoundException;
import com.vaultdesk.backend.security.SecurityUtils;
import com.vaultdesk.backend.user.User;
import com.vaultdesk.backend.user.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Note create(String title, String content) {
        User user = userRepository.getReferenceById(SecurityUtils.currentUserId());
        return noteRepository.save(new Note(user, title, content));
    }

    @Transactional(readOnly = true)
    public Note getOwned(UUID noteId) {
        return noteRepository.findByIdAndUserId(noteId, SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Nota nao encontrada: " + noteId));
    }

    @Transactional
    public Note update(UUID noteId, String title, String content) {
        Note note = getOwned(noteId);
        note.setTitle(title);
        note.setContent(content);
        return note;
    }

    @Transactional
    public void delete(UUID noteId) {
        noteRepository.delete(getOwned(noteId));
    }

    @Transactional(readOnly = true)
    public Page<Note> search(String q, Pageable pageable) {
        String normalizedQuery = (q == null || q.isBlank()) ? null : q.trim();
        return noteRepository.search(SecurityUtils.currentUserId(), normalizedQuery, pageable);
    }
}
