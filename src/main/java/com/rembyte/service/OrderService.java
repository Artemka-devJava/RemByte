package com.rembyte.service;

import com.rembyte.model.Order;
import com.rembyte.model.OrderLine;
import com.rembyte.model.RepairService;
import com.rembyte.repository.OrderAttachmentRepository;
import com.rembyte.repository.OrderRepository;
import com.rembyte.repository.RepairServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления заказами и расчёта стоимости.
 * <p>
 * Состав заказа хранится в {@link OrderLine}: каждая строка — услуга из справочника
 * или разовая позиция, со снимком названия, ценой за единицу и количеством.
 */
@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderAttachmentRepository orderAttachmentRepository;
    private final RepairServiceRepository repairServiceRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderAttachmentRepository orderAttachmentRepository,
                        RepairServiceRepository repairServiceRepository) {
        this.orderRepository = orderRepository;
        this.orderAttachmentRepository = orderAttachmentRepository;
        this.repairServiceRepository = repairServiceRepository;
    }

    /**
     * Создать новый заказ.
     */
    public Order createOrder(Order order) {
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            order.setOrderNumber(generateOrderNumber());
        }
        order.setStatus("NEW");
        if (order.getPaidAmount() == null || order.getPaidAmount() < 0) {
            order.setPaidAmount(0.0);
        }
        applyLines(order, new ArrayList<>(order.getLines()));
        return orderRepository.save(order);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getClientOrders(Long clientId) {
        return orderRepository.findByClientId(clientId);
    }

    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    /**
     * Обновить заказ. Меняются только описание, заметки и состав.
     * Статус и оплата не трогаются — для них есть отдельные операции.
     */
    public Order updateOrder(Long id, Order orderData) {
        return orderRepository.findById(id).map(order -> {
            order.setDeviceDescription(orderData.getDeviceDescription());
            order.setNotes(orderData.getNotes());
            applyLines(order, new ArrayList<>(orderData.getLines()));
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    /**
     * Добавить одну позицию в существующий заказ (без открытия полной формы).
     */
    public Order addLine(Long orderId, OrderLine raw) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        OrderLine line = normalizeLine(raw, order.getLines().size());
        order.addLine(line);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /**
     * Изменить позицию заказа (название разовой позиции, цену, количество).
     */
    public Order updateLine(Long orderId, Long lineId, OrderLine raw) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        OrderLine target = order.getLines().stream()
                .filter(l -> l.getId() != null && l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Строка заказа не найдена"));

        if (raw.getName() != null && !raw.getName().isBlank()) {
            target.setName(raw.getName().trim());
        }
        if (raw.getUnitPrice() != null && raw.getUnitPrice() >= 0) {
            target.setUnitPrice(raw.getUnitPrice());
        }
        target.setQuantity(raw.getQuantity());
        order.recalcTotal();
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /**
     * Удалить позицию из заказа.
     */
    public Order removeLine(Long orderId, Long lineId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        boolean removed = order.removeLine(lineId);
        if (!removed) {
            throw new RuntimeException("Строка заказа не найдена");
        }
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public Order updateOrderStatus(Long orderId, String status) {
        return orderRepository.findById(orderId).map(order -> {
            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());
            if ("COMPLETED".equals(status)) {
                order.setCompletedAt(LocalDateTime.now());
            }
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    public Order addPayment(Long orderId, Double amount) {
        return orderRepository.findById(orderId).map(order -> {
            order.setPaidAmount(order.getPaidAmount() + amount);
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    public Order addAttachmentUrls(Long orderId, List<String> photoUrls, List<String> videoUrls, List<String> fileUrls) {
        return orderRepository.findById(orderId).map(order -> {
            order.addPhotoUrls(photoUrls);
            order.addVideoUrls(videoUrls);
            order.addFileUrls(fileUrls);
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    public Order removeAttachmentUrl(Long orderId, String attachmentUrl) {
        return orderRepository.findById(orderId).map(order -> {
            boolean removed = order.removeAttachmentUrl(attachmentUrl);
            if (!removed) {
                throw new RuntimeException("Вложение не найдено в заказе");
            }
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    public OrderStatistics getStatistics(LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(from, to);

        double totalRevenue = orders.stream()
                .mapToDouble(Order::getTotalPrice)
                .sum();

        double totalPaid = orders.stream()
                .mapToDouble(Order::getPaidAmount)
                .sum();

        long completedCount = orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .count();

        return new OrderStatistics(
                orders.size(),
                completedCount,
                totalRevenue,
                totalPaid,
                orders.isEmpty() ? 0 : totalRevenue / orders.size()
        );
    }

    public void deleteOrder(Long id) {
        orderAttachmentRepository.deleteByOrderId(id);
        orderRepository.deleteById(id);
    }

    private String generateOrderNumber() {
        return "ФБ-" + System.currentTimeMillis();
    }

    // ===== Работа с составом заказа =====

    /**
     * Полностью заменить состав заказа нормализованными строками.
     */
    private void applyLines(Order order, List<OrderLine> rawLines) {
        List<OrderLine> normalized = new ArrayList<>();
        if (rawLines != null) {
            int i = 0;
            for (OrderLine raw : rawLines) {
                if (raw == null) continue;
                normalized.add(normalizeLine(raw, i++));
            }
        }
        order.setLines(normalized);
    }

    /**
     * Привести пришедшую с фронта строку к сохраняемому виду:
     * подтянуть управляемую сущность услуги, зафиксировать снимок названия и цену.
     */
    private OrderLine normalizeLine(OrderLine raw, int index) {
        OrderLine line = new OrderLine();
        line.setSortOrder(index);
        line.setQuantity(raw.getQuantity());

        RepairService ref = null;
        if (raw.getService() != null && raw.getService().getId() != null) {
            ref = repairServiceRepository.findById(raw.getService().getId()).orElse(null);
        }
        line.setService(ref);

        String name = raw.getName() != null && !raw.getName().isBlank()
                ? raw.getName().trim()
                : (ref != null ? ref.getName() : null);
        line.setName(name == null || name.isBlank() ? "Позиция" : name);

        Double price = raw.getUnitPrice();
        if ((price == null || price < 0) && ref != null) {
            price = ref.getBasePrice();
        }
        line.setUnitPrice(price == null || price < 0 ? 0.0 : price);

        return line;
    }
}
