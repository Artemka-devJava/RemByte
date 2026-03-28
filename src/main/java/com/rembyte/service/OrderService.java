package com.rembyte.service;

import com.rembyte.model.Order;
import com.rembyte.model.RepairService;
import com.rembyte.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для управления заказами и расчета стоимости
 */
@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Создать новый заказ
     */
    public Order createOrder(Order order) {
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            order.setOrderNumber(generateOrderNumber());
        }
        order.setStatus("NEW");
        return orderRepository.save(order);
    }

    /**
     * Получить заказ по ID
     */
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    /**
     * Получить все заказы
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Получить заказы клиента
     */
    public List<Order> getClientOrders(Long clientId) {
        return orderRepository.findByClientId(clientId);
    }

    /**
     * Найти заказ по номеру
     */
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    /**
     * Обновить заказ
     */
    public Order updateOrder(Long id, Order orderData) {
        return orderRepository.findById(id).map(order -> {
            order.setDeviceDescription(orderData.getDeviceDescription());
            order.setStatus(orderData.getStatus());
            order.setServices(orderData.getServices());
            order.setNotes(orderData.getNotes());
            order.setUpdatedAt(LocalDateTime.now());
            order.setTotalPrice(calculateOrderPrice(order.getServices()));
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    /**
     * Добавить услугу к заказу
     */
    public Order addServiceToOrder(Long orderId, RepairService service) {
        return orderRepository.findById(orderId).map(order -> {
            order.getServices().add(service);
            order.setTotalPrice(calculateOrderPrice(order.getServices()));
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    /**
     * Удалить услугу из заказа
     */
    public Order removeServiceFromOrder(Long orderId, Long serviceId) {
        return orderRepository.findById(orderId).map(order -> {
            order.setServices(order.getServices().stream()
                    .filter(s -> !s.getId().equals(serviceId))
                    .collect(Collectors.toSet()));
            order.setTotalPrice(calculateOrderPrice(order.getServices()));
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    /**
     * Обновить статус заказа
     */
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

    /**
     * Добавить платеж
     */
    public Order addPayment(Long orderId, Double amount) {
        return orderRepository.findById(orderId).map(order -> {
            order.setPaidAmount(order.getPaidAmount() + amount);
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    /**
     * Добавить ссылки на фото/видео/документы к заказу
     */
    public Order addAttachmentUrls(Long orderId, List<String> photoUrls, List<String> videoUrls, List<String> fileUrls) {
        return orderRepository.findById(orderId).map(order -> {
            order.addPhotoUrls(photoUrls);
            order.addVideoUrls(videoUrls);
            order.addFileUrls(fileUrls);
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    /**
     * Удалить ссылку вложения у заказа
     */
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

    /**
     * Получить статистику за период
     */
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

    /**
     * Удалить заказ
     */
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    /**
     * Генерировать уникальный номер заказа
     */
    private String generateOrderNumber() {
        return "ФБ-" + System.currentTimeMillis();
    }

    /**
     * Рассчитать стоимость заказа
     */
    public Double calculateOrderPrice(Set<RepairService> services) {
        if (services == null || services.isEmpty()) {
            return 0.0;
        }
        return services.stream()
                .mapToDouble(RepairService::getBasePrice)
                .sum();
    }
}



