package com.rembyte.repository;

import com.rembyte.model.KanbanCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KanbanCardRepository extends JpaRepository<KanbanCard, Long> {
    List<KanbanCard> findByColumnIdOrderByPositionAscIdAsc(Long columnId);
    long countByColumnId(Long columnId);
}

