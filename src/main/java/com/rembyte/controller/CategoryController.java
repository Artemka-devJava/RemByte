package com.rembyte.controller;

import com.rembyte.model.ServiceCategory;
import com.rembyte.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API для управления категориями услуг
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<ServiceCategory>> getAll() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody ServiceCategory category) {
        if (categoryRepository.existsByName(category.getName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Категория уже существует"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ServiceCategory data) {
        return categoryRepository.findById(id).map(cat -> {
            cat.setName(data.getName());
            cat.setIcon(data.getIcon() != null ? data.getIcon() : "🔧");
            return ResponseEntity.ok(categoryRepository.save(cat));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

