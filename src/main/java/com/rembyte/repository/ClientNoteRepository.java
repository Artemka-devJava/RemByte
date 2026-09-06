package com.rembyte.repository;

import com.rembyte.model.ClientNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientNoteRepository extends JpaRepository<ClientNote, Long> {
    List<ClientNote> findByClient_IdOrderByCreatedAtDesc(Long clientId);
    void deleteByClient_Id(Long clientId);
}
