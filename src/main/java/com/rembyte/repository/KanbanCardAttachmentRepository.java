package com.rembyte.repository;

import com.rembyte.model.KanbanCardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KanbanCardAttachmentRepository extends JpaRepository<KanbanCardAttachment, Long> {
    List<KanbanCardAttachment> findByCardIdOrderByCreatedAtAscIdAsc(Long cardId);
    Optional<KanbanCardAttachment> findByCardIdAndStoredName(Long cardId, String storedName);
    long deleteByCardIdAndStoredName(Long cardId, String storedName);
}

