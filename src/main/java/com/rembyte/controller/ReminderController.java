package com.rembyte.controller;

import com.rembyte.service.ReminderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Напоминания оператору: ручные + авто «готово, но не забрали».
 */
@RestController
@RequestMapping("/api/reminders")
@CrossOrigin(origins = "*")
public class ReminderController {

    private final ReminderService service;

    public ReminderController(ReminderService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reminders", service.open());
        m.put("staleOrders", service.staleOrders());
        m.put("staleOrderDays", service.staleOrderDays());
        return m;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest body) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(service.create(body.text(), body.dueAt(), body.clientId(), body.orderId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/done")
    public ResponseEntity<?> markDone(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.markDone(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(
            String text,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueAt,
            Long clientId,
            Long orderId) {}
}
