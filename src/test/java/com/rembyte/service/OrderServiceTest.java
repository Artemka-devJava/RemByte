package com.rembyte.service;

import com.rembyte.model.Order;
import com.rembyte.model.OrderLine;
import com.rembyte.model.RepairService;
import com.rembyte.repository.OrderAttachmentRepository;
import com.rembyte.repository.OrderRepository;
import com.rembyte.repository.PaymentRepository;
import com.rembyte.repository.ReminderRepository;
import com.rembyte.repository.RepairServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderAttachmentRepository orderAttachmentRepository;
    @Mock RepairServiceRepository repairServiceRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ReminderRepository reminderRepository;

    OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, orderAttachmentRepository, repairServiceRepository,
                paymentRepository, reminderRepository);
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static OrderLine rawLine(String name, Double price, Integer qty) {
        OrderLine l = new OrderLine();
        l.setName(name);
        if (price != null) l.setUnitPrice(price);
        if (qty != null) l.setQuantity(qty);
        return l;
    }

    @Test
    void createOrder_generatesNumberForcesNewStatusAndClampsPaid() {
        Order in = new Order();
        in.setPaidAmount(-100.0);

        Order out = service.createOrder(in);

        assertThat(out.getOrderNumber()).startsWith("ФБ-");
        assertThat(out.getStatus()).isEqualTo("NEW");
        assertThat(out.getPaidAmount()).isEqualTo(0.0);
    }

    @Test
    void deleteOrder_cascadesPaymentsRemindersAndAttachments() {
        service.deleteOrder(7L);

        org.mockito.Mockito.verify(paymentRepository).deleteByOrderId(7L);
        org.mockito.Mockito.verify(reminderRepository).deleteByOrderId(7L);
        org.mockito.Mockito.verify(orderAttachmentRepository).deleteByOrderId(7L);
        org.mockito.Mockito.verify(orderRepository).deleteById(7L);
    }

    @Test
    void createOrder_keepsCallerSuppliedOrderNumber() {
        Order in = new Order();
        in.setOrderNumber("ФБ-CUSTOM-1");

        assertThat(service.createOrder(in).getOrderNumber()).isEqualTo("ФБ-CUSTOM-1");
    }

    @Test
    void createOrder_normalizesLinesAndComputesTotal() {
        Order in = new Order();
        in.getLines().add(rawLine("Диагностика", 500.0, 2));
        in.getLines().add(rawLine("Пайка", 1000.0, 1));

        Order out = service.createOrder(in);

        assertThat(out.getLines()).extracting(OrderLine::getSortOrder).containsExactly(0, 1);
        assertThat(out.getTotalPrice()).isEqualTo(2000.0);
    }

    @Test
    void createOrder_lineWithoutNameFallsBackToServiceNameAndBasePrice() {
        RepairService svc = new RepairService("Замена экрана", 3000.0, "Экран");
        svc.setId(42L);
        when(repairServiceRepository.findById(42L)).thenReturn(Optional.of(svc));

        OrderLine raw = new OrderLine();
        raw.setService(svc);          // id present, no explicit name / price
        raw.setUnitPrice(-1.0);       // treated as "not provided"
        raw.setQuantity(1);
        Order in = new Order();
        in.getLines().add(raw);

        Order out = service.createOrder(in);

        assertThat(out.getLines()).hasSize(1);
        assertThat(out.getLines().get(0).getName()).isEqualTo("Замена экрана");
        assertThat(out.getLines().get(0).getUnitPrice()).isEqualTo(3000.0);
        assertThat(out.getLines().get(0).getService()).isSameAs(svc);
    }

    @Test
    void createOrder_blankLineNameWithoutServiceBecomesPlaceholder() {
        OrderLine raw = new OrderLine();
        raw.setName("   ");
        raw.setUnitPrice(0.0);
        Order in = new Order();
        in.getLines().add(raw);

        Order out = service.createOrder(in);

        assertThat(out.getLines().get(0).getName()).isEqualTo("Позиция");
    }

    @Test
    void updateOrderStatus_setsCompletedAtOnlyForCompleted() {
        Order order = new Order();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        service.updateOrderStatus(1L, "IN_PROGRESS");
        assertThat(order.getCompletedAt()).isNull();

        service.updateOrderStatus(1L, "COMPLETED");
        assertThat(order.getCompletedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void addPayment_accumulatesOntoPaidAmount() {
        Order order = new Order();
        order.setPaidAmount(100.0);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        service.addPayment(1L, 250.0);

        assertThat(order.getPaidAmount()).isEqualTo(350.0);
    }

    @Test
    void mutatingOperationsOnMissingOrderThrow() {
        when(orderRepository.findById(77L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateOrderStatus(77L, "NEW")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.addPayment(77L, 10.0)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.addLine(77L, new OrderLine())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getStatistics_aggregatesRevenuePaidCompletedAndAverage() {
        Order a = new Order(); a.setTotalPrice(1000.0); a.setPaidAmount(1000.0); a.setStatus("COMPLETED");
        Order b = new Order(); b.setTotalPrice(3000.0); b.setPaidAmount(500.0);  b.setStatus("IN_PROGRESS");
        when(orderRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(a, b));

        OrderStatistics stats = service.getStatistics(null, null);

        assertThat(stats.getTotalOrders()).isEqualTo(2);
        assertThat(stats.getCompletedOrders()).isEqualTo(1);
        assertThat(stats.getTotalRevenue()).isEqualTo(4000.0);
        assertThat(stats.getTotalPaid()).isEqualTo(1500.0);
        assertThat(stats.getAverageOrderPrice()).isEqualTo(2000.0);
    }
}
