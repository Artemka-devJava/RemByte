package com.rembyte.repository;

import com.rembyte.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);
    long deleteByOrderId(Long orderId);
}

