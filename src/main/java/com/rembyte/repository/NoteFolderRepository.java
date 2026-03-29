package com.rembyte.repository;

import com.rembyte.model.NoteFolder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteFolderRepository extends JpaRepository<NoteFolder, Long> {
    boolean existsByNameIgnoreCase(String name);
}

