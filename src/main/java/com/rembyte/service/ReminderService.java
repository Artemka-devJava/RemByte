package com.rembyte.service;

import com.rembyte.model.Order;
import com.rembyte.model.Reminder;
import com.rembyte.repository.OrderRepository;
import com.rembyte.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Напоминания оператору: ручные («перезвонить по заявке») и авто-обнаружение
 * «готовых заказов, которые давно не забрали».
 */
@Service
@Transactional
public class ReminderService {

    /** Статусы «работа сделана, ждём выдачи». */
    private static final List<String> DONE_NOT_ISSUED = List.of("READY", "COMPLETED");

    private final ReminderRepository reminders;
    private final OrderRepository orders;

    @Value("${fixbyte.reminders.stale-order-days:30}")
    private int staleOrderDays;

    public ReminderService(ReminderRepository reminders, OrderRepository orders) {
        this.reminders = reminders;
        this.orders = orders;
    }

    // ── ручные напоминания ──────────────────────────────────

    public List<Reminder> open() {
        return reminders.findByDoneFalseOrderByDueAtAscIdAsc();
    }

    public List<Reminder> forOrder(Long orderId) {
        return reminders.findByOrderIdOrderByIdDesc(orderId);
    }

    public Reminder create(String text, LocalDateTime dueAt, Long clientId, Long orderId) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Текст напоминания обязателен");
        }
        Reminder r = new Reminder();
        r.setText(t);
        r.setDueAt(dueAt);
        r.setClientId(clientId);
        r.setOrderId(orderId);
        r.setCreatedAt(LocalDateTime.now());
        return reminders.save(r);
    }

    public Reminder markDone(Long id) {
        Reminder r = reminders.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Напоминание не найдено"));
        r.setDone(true);
        r.setDoneAt(LocalDateTime.now());
        return reminders.save(r);
    }

    public void delete(Long id) {
        reminders.deleteById(id);
    }

    // ── авто: «готово, но не забрали» ───────────────────────

    @Transactional(readOnly = true)
    public List<StaleOrder> staleOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(Math.max(1, staleOrderDays));
        List<StaleOrder> out = new ArrayList<>();
        for (String status : DONE_NOT_ISSUED) {
            for (Order o : orders.findByStatus(status)) {
                LocalDateTime since = latest(o.getCompletedAt(), o.getUpdatedAt(), o.getCreatedAt());
                if (since == null || since.isAfter(threshold)) {
                    continue;
                }
                out.add(new StaleOrder(
                        o.getId(),
                        o.getOrderNumber(),
                        o.getClient() != null ? o.getClient().getName() : "—",
                        o.getClient() != null ? o.getClient().getId() : null,
                        status,
                        since
                ));
            }
        }
        out.sort(Comparator.comparing(StaleOrder::since));
        return out;
    }

    public int staleOrderDays() {
        return staleOrderDays;
    }

    private static LocalDateTime latest(LocalDateTime... values) {
        LocalDateTime max = null;
        for (LocalDateTime v : values) {
            if (v != null && (max == null || v.isAfter(max))) {
                max = v;
            }
        }
        return max;
    }

    public record StaleOrder(Long orderId, String orderNumber, String clientName,
                             Long clientId, String status, LocalDateTime since) {}
}
