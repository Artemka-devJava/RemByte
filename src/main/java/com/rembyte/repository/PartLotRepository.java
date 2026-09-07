package com.rembyte.repository;

import com.rembyte.model.PartLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PartLotRepository extends JpaRepository<PartLot, Long> {
    List<PartLot> findAllByOrderByCreatedAtDesc();
    List<PartLot> findByStatusAndSaleDateBetween(String status, LocalDateTime from, LocalDateTime to);
}
