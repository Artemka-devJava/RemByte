package com.rembyte.repository;

import com.rembyte.model.RepairService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepairServiceRepository extends JpaRepository<RepairService, Long> {
    Optional<RepairService> findByName(String name);
    List<RepairService> findByCategory(String category);
    List<RepairService> findByIsActiveTrue();
}

