package com.rembyte.repository;

import com.rembyte.model.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<ServiceCategory, Long> {
    Optional<ServiceCategory> findByName(String name);
    boolean existsByName(String name);
}

