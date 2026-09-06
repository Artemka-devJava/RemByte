package com.rembyte.repository;

import com.rembyte.model.ClientDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientDeviceRepository extends JpaRepository<ClientDevice, Long> {
    List<ClientDevice> findByClient_IdOrderByCreatedAtDesc(Long clientId);
    void deleteByClient_Id(Long clientId);
}
