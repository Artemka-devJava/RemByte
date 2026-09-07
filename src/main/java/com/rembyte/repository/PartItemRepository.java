package com.rembyte.repository;

import com.rembyte.model.PartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PartItemRepository extends JpaRepository<PartItem, Long> {
    List<PartItem> findByStatusOrderByCreatedAtDesc(String status);
    List<PartItem> findAllByOrderByCreatedAtDesc();
    List<PartItem> findByLotIsNull();
    List<PartItem> findByLot_Id(Long lotId);
    List<PartItem> findByPurchaseDateBetween(LocalDateTime from, LocalDateTime to);
    List<PartItem> findByStatusAndSaleDateBetween(String status, LocalDateTime from, LocalDateTime to);
}
