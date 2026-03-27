package com.rembyte.controller;

import com.rembyte.model.RepairService;
import com.rembyte.service.RepairServiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST контроллер для управления услугами ремонта
 */
@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "*")
public class RepairServiceController {
    private final RepairServiceService serviceService;

    public RepairServiceController(RepairServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @PostMapping
    public ResponseEntity<RepairService> createService(@RequestBody RepairService service) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceService.createService(service));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepairService> getService(@PathVariable Long id) {
        return serviceService.getServiceById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<RepairService>> getAllServices() {
        return ResponseEntity.ok(serviceService.getAllServices());
    }

    @GetMapping("/active")
    public ResponseEntity<List<RepairService>> getActiveServices() {
        return ResponseEntity.ok(serviceService.getActiveServices());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<RepairService>> getServicesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(serviceService.getServicesByCategory(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RepairService> updateService(@PathVariable Long id, 
                                                       @RequestBody RepairService serviceData) {
        try {
            return ResponseEntity.ok(serviceService.updateService(id, serviceData));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}

