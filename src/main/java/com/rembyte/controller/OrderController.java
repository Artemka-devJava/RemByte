package com.rembyte.controller;

import com.rembyte.model.Order;
import com.rembyte.model.RepairService;
import com.rembyte.service.OrderService;
import com.rembyte.service.OrderStatistics;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST контроллер для управления заказами
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Order>> getClientOrders(@PathVariable Long clientId) {
        return ResponseEntity.ok(orderService.getClientOrders(clientId));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        return orderService.findByOrderNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order orderData) {
        try {
            return ResponseEntity.ok(orderService.updateOrder(id, orderData));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/add-service/{serviceId}")
    public ResponseEntity<Order> addServiceToOrder(@PathVariable Long id, 
                                                    @PathVariable Long serviceId) {
        try {
            // Nota: В реальном приложении нужно получить сервис из БД
            return ResponseEntity.ok(orderService.getOrderById(id).get());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<Order> addPayment(@PathVariable Long id, @RequestParam Double amount) {
        try {
            return ResponseEntity.ok(orderService.addPayment(id, amount));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<OrderStatistics> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(orderService.getStatistics(from, to));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}

