package com.rembyte.repository;

import com.rembyte.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByPhone(String phone);
    Optional<Client> findByEmail(String email);
    List<Client> findByNameContainingIgnoreCase(String name);
    List<Client> findByIsActiveTrue();

    @Query("select c from Client c where " +
           "lower(c.name) like lower(concat('%', :q, '%')) " +
           "or c.phone like concat('%', :q, '%') " +
           "or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%')) " +
           "order by c.name asc")
    List<Client> searchByNameOrPhoneOrEmail(@Param("q") String q);
}

