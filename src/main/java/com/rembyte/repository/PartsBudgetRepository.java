package com.rembyte.repository;

import com.rembyte.model.PartsBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartsBudgetRepository extends JpaRepository<PartsBudget, Long> {
}
