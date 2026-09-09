package com.rembyte.service;

import com.rembyte.model.Client;
import com.rembyte.model.Order;
import com.rembyte.model.Reminder;
import com.rembyte.repository.OrderRepository;
import com.rembyte.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock ReminderRepository reminders;
    @Mock OrderRepository orders;
    ReminderService service;

    @BeforeEach
    void setUp() {
        service = new ReminderService(reminders, orders);
        ReflectionTestUtils.setField(service, "staleOrderDays", 30);
        lenient().when(reminders.save(any(Reminder.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void create_rejectsBlankTextAndTrims() {
        assertThatThrownBy(() -> service.create("   ", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        Reminder r = service.create("  перезвонить  ", LocalDateTime.now().plusDays(1), 5L, 9L);
        assertThat(r.getText()).isEqualTo("перезвонить");
        assertThat(r.getClientId()).isEqualTo(5L);
        assertThat(r.getOrderId()).isEqualTo(9L);
        assertThat(r.isDone()).isFalse();
    }

    @Test
    void markDone_setsFlagAndTimestamp() {
        Reminder r = new Reminder();
        r.setText("x");
        when(reminders.findById(1L)).thenReturn(Optional.of(r));

        Reminder done = service.markDone(1L);

        assertThat(done.isDone()).isTrue();
        assertThat(done.getDoneAt()).isNotNull();
    }

    @Test
    void markDone_missingThrows() {
        when(reminders.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markDone(7L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void staleOrders_onlyReadyOrCompletedOlderThanThreshold() {
        Client c = new Client("Иван", "+79990000001");
        c.setId(3L);

        Order fresh = order(1L, "READY", c, LocalDateTime.now().minusDays(5));
        Order stale = order(2L, "READY", c, LocalDateTime.now().minusDays(45));
        Order completedStale = order(3L, "COMPLETED", c, LocalDateTime.now().minusDays(90));

        when(orders.findByStatus("READY")).thenReturn(List.of(fresh, stale));
        when(orders.findByStatus("COMPLETED")).thenReturn(List.of(completedStale));

        List<ReminderService.StaleOrder> list = service.staleOrders();

        assertThat(list).extracting(ReminderService.StaleOrder::orderId).containsExactly(3L, 2L); // старейший первым
        assertThat(list).allMatch(s -> "Иван".equals(s.clientName()));
    }

    @Test
    void staleOrders_usesLatestOfCompletedUpdatedCreated() {
        Client c = new Client("П", "+70000000000");
        Order o = new Order("ФБ-1", c);
        o.setId(1L);
        o.setStatus("COMPLETED");
        o.setCreatedAt(LocalDateTime.now().minusDays(100));
        o.setUpdatedAt(LocalDateTime.now().minusDays(2));      // недавно трогали
        o.setCompletedAt(LocalDateTime.now().minusDays(100));
        when(orders.findByStatus("READY")).thenReturn(List.of());
        when(orders.findByStatus("COMPLETED")).thenReturn(List.of(o));

        assertThat(service.staleOrders()).isEmpty(); // свежий updatedAt → не «залежался»
    }

    @Test
    void open_delegatesToRepo() {
        when(reminders.findByDoneFalseOrderByDueAtAscIdAsc()).thenReturn(List.of(new Reminder()));
        assertThat(service.open()).hasSize(1);
        verify(reminders).findByDoneFalseOrderByDueAtAscIdAsc();
    }

    private static Order order(long id, String status, Client c, LocalDateTime when) {
        Order o = new Order("ФБ-" + id, c);
        o.setId(id);
        o.setStatus(status);
        o.setCreatedAt(when);
        o.setUpdatedAt(when);
        return o;
    }
}
