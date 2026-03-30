package com.rembyte.repository;

import com.rembyte.model.KanbanBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KanbanBoardRepository extends JpaRepository<KanbanBoard, Long> {
    Optional<KanbanBoard> findFirstByOwnerUsernameOrderByUpdatedAtDescIdAsc(String ownerUsername);
    Optional<KanbanBoard> findByIdAndOwnerUsername(Long id, String ownerUsername);
    List<KanbanBoard> findByOwnerUsernameOrderByUpdatedAtDescIdAsc(String ownerUsername);
}
