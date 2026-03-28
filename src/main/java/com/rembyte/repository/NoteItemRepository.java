package com.rembyte.repository;

import com.rembyte.model.NoteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteItemRepository extends JpaRepository<NoteItem, Long> {
    List<NoteItem> findByFolderIdOrderByUpdatedAtDesc(Long folderId);
    long countByFolderId(Long folderId);
    void deleteByFolderId(Long folderId);
}

