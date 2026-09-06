package com.rembyte.controller;

import com.rembyte.model.Order;
import com.rembyte.model.OrderLine;
import com.rembyte.service.FileStorageService;
import com.rembyte.service.OrderService;
import com.rembyte.service.OrderStatistics;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
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
    private final FileStorageService fileStorageService;

    public OrderController(OrderService orderService, FileStorageService fileStorageService) {
        this.orderService = orderService;
        this.fileStorageService = fileStorageService;
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

    @PostMapping("/{id}/lines")
    public ResponseEntity<?> addLine(@PathVariable Long id, @RequestBody OrderLine line) {
        try {
            return ResponseEntity.ok(orderService.addLine(id, line));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/lines/{lineId}")
    public ResponseEntity<?> updateLine(@PathVariable Long id,
                                       @PathVariable Long lineId,
                                       @RequestBody OrderLine line) {
        try {
            return ResponseEntity.ok(orderService.updateLine(id, lineId, line));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    public ResponseEntity<?> deleteLine(@PathVariable Long id, @PathVariable Long lineId) {
        try {
            return ResponseEntity.ok(orderService.removeLine(id, lineId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
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

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAttachments(@PathVariable Long id,
                                               @RequestParam("files") MultipartFile[] files) {
        try {
            FileStorageService.UploadResult uploaded = fileStorageService.saveOrderAttachments(id, files);
            return ResponseEntity.ok(orderService.addAttachmentUrls(id, uploaded.photoUrls(), uploaded.videoUrls(), uploaded.fileUrls()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Заказ не найден");
        }
    }

    @DeleteMapping("/{id}/attachments")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long id,
                                              @RequestParam("url") String attachmentUrl) {
        try {
            fileStorageService.deleteOrderAttachment(id, attachmentUrl);
            return ResponseEntity.ok(orderService.removeAttachmentUrl(id, attachmentUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
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

