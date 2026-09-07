package com.rembyte.controller;

import com.rembyte.model.PartItem;
import com.rembyte.model.PartLot;
import com.rembyte.model.PartsBudget;
import com.rembyte.service.PartsService;
import com.rembyte.service.PartsStatistics;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST-контроллер учёта комплектующих: закупка/продажа деталей и сборных лотов.
 */
@RestController
@RequestMapping("/api/parts")
@CrossOrigin(origins = "*")
public class PartsController {

    private final PartsService partsService;

    public PartsController(PartsService partsService) {
        this.partsService = partsService;
    }

    // ── Детали ─────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<PartItem>> getAllItems(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(partsService.getAllItems(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartItem> getItem(@PathVariable Long id) {
        return partsService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PartItem> createItem(@RequestBody PartItem data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partsService.createItem(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody PartItem data) {
        try {
            return ResponseEntity.ok(partsService.updateItem(id, data));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        partsService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sell")
    public ResponseEntity<?> sellItem(@PathVariable Long id, @RequestBody SellRequest body) {
        try {
            return ResponseEntity.ok(partsService.sellItem(id, body.salePrice(), body.saleDate()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ── Фото деталей ───────────────────────────────────────────

    @GetMapping("/{id}/photos")
    public ResponseEntity<?> listPhotos(@PathVariable Long id) {
        return ResponseEntity.ok(partsService.listPhotos(id));
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addPhotos(@PathVariable Long id, @RequestParam("files") MultipartFile[] files) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(partsService.addPhotos(id, files));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/photos/{photoId}/raw")
    public ResponseEntity<byte[]> rawPhoto(@PathVariable Long id, @PathVariable Long photoId) {
        return partsService.getPhotoData(id, photoId)
                .map(data -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(
                                data.contentType() == null ? "application/octet-stream" : data.contentType()))
                        .body(data.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        partsService.deletePhoto(id, photoId);
        return ResponseEntity.noContent().build();
    }

    // ── Лоты ───────────────────────────────────────────────────

    @GetMapping("/lots")
    public ResponseEntity<List<PartLot>> getAllLots() {
        return ResponseEntity.ok(partsService.getAllLots());
    }

    @GetMapping("/lots/{id}")
    public ResponseEntity<PartLot> getLot(@PathVariable Long id) {
        return partsService.getLotById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/lots")
    public ResponseEntity<?> createLot(@RequestBody LotRequest body) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(partsService.createLot(body.title(), body.itemIds()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/lots/{id}")
    public ResponseEntity<?> updateLot(@PathVariable Long id, @RequestBody LotRequest body) {
        try {
            return ResponseEntity.ok(partsService.updateLotItems(id, body.title(), body.itemIds()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/lots/{id}")
    public ResponseEntity<?> disbandLot(@PathVariable Long id) {
        try {
            partsService.disbandLot(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/lots/{id}/sell")
    public ResponseEntity<?> sellLot(@PathVariable Long id, @RequestBody SellRequest body) {
        try {
            return ResponseEntity.ok(partsService.sellLot(id, body.salePrice(), body.saleDate()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ── Бюджет и статистика ────────────────────────────────────

    @GetMapping("/budget")
    public ResponseEntity<PartsBudget> getBudget() {
        return ResponseEntity.ok(partsService.getBudget());
    }

    @PutMapping("/budget")
    public ResponseEntity<PartsBudget> setBudget(@RequestBody BudgetRequest body) {
        return ResponseEntity.ok(partsService.setStartingAmount(body.startingAmount()));
    }

    @GetMapping("/stats")
    public ResponseEntity<PartsStatistics> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(partsService.getStatistics(from, to));
    }

    // ── DTO запросов ───────────────────────────────────────────

    public record SellRequest(Double salePrice, LocalDateTime saleDate) {}
    public record LotRequest(String title, List<Long> itemIds) {}
    public record BudgetRequest(Double startingAmount) {}
}
