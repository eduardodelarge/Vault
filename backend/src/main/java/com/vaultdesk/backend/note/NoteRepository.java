package com.vaultdesk.backend.note;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    Optional<Note> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Quando {@code q} e nulo/em branco, retorna todas as notas do usuario (list). Quando
     * preenchido, filtra por titulo (case-insensitive), aproveitando o indice
     * "idx_notes_title_trgm" (pg_trgm) criado na migracao V1.
     *
     * <p>Ordenacao fixa em {@code updated_at DESC} propositalmente: numa {@code @Query} nativa o
     * Spring Data injeta o {@link Sort} do {@link Pageable} como nome de coluna SQL literal (sem
     * mapear "updatedAt" -> "updated_at", e sem validar contra colunas reais), entao {@code
     * pageable} aqui deve chegar sempre sem Sort (ver NoteController) para nao virar um vetor de
     * injecao via parametro "sort" vindo do cliente.
     */
    @Query(
            value = "SELECT * FROM notes WHERE user_id = :userId "
                    + "AND (:q IS NULL OR title ILIKE CONCAT('%', :q, '%')) "
                    + "ORDER BY updated_at DESC",
            countQuery = "SELECT count(*) FROM notes WHERE user_id = :userId "
                    + "AND (:q IS NULL OR title ILIKE CONCAT('%', :q, '%'))",
            nativeQuery = true)
    Page<Note> search(@Param("userId") UUID userId, @Param("q") String q, Pageable pageable);
}
