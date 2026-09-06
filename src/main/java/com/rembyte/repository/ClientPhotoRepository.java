package com.rembyte.repository;

import com.rembyte.model.ClientPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientPhotoRepository extends JpaRepository<ClientPhoto, Long> {
    List<ClientPhoto> findByClient_IdOrderByCreatedAtDesc(Long clientId);
    void deleteByClient_Id(Long clientId);
}
