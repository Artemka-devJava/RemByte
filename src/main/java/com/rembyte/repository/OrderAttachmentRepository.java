package com.rembyte.repository;

import com.rembyte.model.OrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderAttachmentRepository extends JpaRepository<OrderAttachment, Long> {
    Optional<OrderAttachment> findByOrderIdAndStoredName(Long orderId, String storedName);

    boolean existsByOrderIdAndStoredName(Long orderId, String storedName);

    long deleteByOrderIdAndStoredName(Long orderId, String storedName);

    long deleteByOrderId(Long orderId);
}

