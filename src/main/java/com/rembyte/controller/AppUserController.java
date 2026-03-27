package com.rembyte.controller;

import com.rembyte.model.AppUser;
import com.rembyte.service.AppUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API для управления пользователями (только ADMIN)
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AppUserController {

    private final AppUserService userService;

    public AppUserController(AppUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<AppUser>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppUser> getUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody AppUser user) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody AppUser user) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

