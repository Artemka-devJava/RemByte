package com.rembyte.service;

import com.rembyte.model.Order;
import com.rembyte.model.Payment;
import com.rembyte.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;

    @Test
    void getTotalPayments_sumsAmountsInRange() {
        PaymentService service = new PaymentService(paymentRepository);
        Order order = new Order();
        when(paymentRepository.findByPaymentDateBetween(any(), any()))
                .thenReturn(List.of(new Payment(order, 100.0), new Payment(order, 250.5), new Payment(order, 0.0)));

        double total = service.getTotalPayments(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 12, 31, 23, 59));

        assertThat(total).isEqualTo(350.5);
    }

    @Test
    void getTotalPayments_emptyRangeIsZero() {
        PaymentService service = new PaymentService(paymentRepository);
        when(paymentRepository.findByPaymentDateBetween(any(), any())).thenReturn(List.of());

        assertThat(service.getTotalPayments(LocalDateTime.now().minusDays(1), LocalDateTime.now()))
                .isEqualTo(0.0);
    }
}
