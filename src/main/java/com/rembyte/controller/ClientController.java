package com.rembyte.controller;

import com.rembyte.model.Client;
import com.rembyte.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST контроллер для управления клиентами
 */
@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {
    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientService.createClient(client));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClient(@PathVariable Long id) {
        return clientService.getClientById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Client>> getActiveClients() {
        return ResponseEntity.ok(clientService.getActiveClients());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Client>> searchClients(@RequestParam String name) {
        return ResponseEntity.ok(clientService.searchByName(name));
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<Client> getClientByPhone(@PathVariable String phone) {
        return clientService.findByPhone(phone)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @RequestBody Client clientData) {
        try {
            return ResponseEntity.ok(clientService.updateClient(id, clientData));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateClient(@PathVariable Long id) {
        try {
            clientService.deactivateClient(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}

