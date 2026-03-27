package com.rembyte.repository;

import com.rembyte.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByPhone(String phone);
    Optional<Client> findByEmail(String email);
    List<Client> findByNameContainingIgnoreCase(String name);
    List<Client> findByIsActiveTrue();
}

