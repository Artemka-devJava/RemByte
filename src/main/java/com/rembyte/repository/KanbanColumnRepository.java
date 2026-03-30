package com.rembyte.repository;

import com.rembyte.model.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {
    List<KanbanColumn> findByBoardIdOrderByPositionAscIdAsc(Long boardId);
}

