package com.rembyte.repository;

import com.rembyte.model.PartItemPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartItemPhotoRepository extends JpaRepository<PartItemPhoto, Long> {
    List<PartItemPhoto> findByItem_IdOrderByCreatedAtDesc(Long itemId);
    void deleteByItem_Id(Long itemId);
}
