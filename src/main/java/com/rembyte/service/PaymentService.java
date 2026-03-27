package com.rembyte.service;

import com.rembyte.model.Payment;
import com.rembyte.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для управления платежами
 */
@Service
@Transactional
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment recordPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> getPaymentsByDateRange(LocalDateTime from, LocalDateTime to) {
        return paymentRepository.findByPaymentDateBetween(from, to);
    }

    public Double getTotalPayments(LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(from, to);
        return payments.stream()
                .mapToDouble(Payment::getAmount)
                .sum();
    }
}

