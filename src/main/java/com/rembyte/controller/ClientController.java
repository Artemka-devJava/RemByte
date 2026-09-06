package com.rembyte.controller;

import com.rembyte.model.Client;
import com.rembyte.model.ClientDevice;
import com.rembyte.model.ClientNote;
import com.rembyte.service.ClientService;
import com.rembyte.service.ClientSummary;
import com.rembyte.service.DuplicateClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * REST-контроллер ведения клиентов: карточка, журнал, устройства, фото, архив.
 */
@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {
    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    // ── Карточка ────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createClient(@RequestBody Client client) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(client));
        } catch (DuplicateClientException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getExisting());
        }
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

    @GetMapping("/duplicates")
    public ResponseEntity<List<Client>> duplicates(@RequestParam String phone,
                                                   @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(clientService.findPossibleDuplicates(phone, excludeId));
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<Client> getClientByPhone(@PathVariable String phone) {
        return clientService.findByPhone(phone)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateClient(@PathVariable Long id, @RequestBody Client clientData) {
        try {
            return ResponseEntity.ok(clientService.updateClient(id, clientData));
        } catch (DuplicateClientException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getExisting());
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

    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(clientService.archiveClient(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(clientService.restoreClient(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ClientSummary> summary(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.summary(id));
    }

    // ── Журнал ─────────────────────────────────────────────────

    @GetMapping("/{id}/notes")
    public ResponseEntity<List<ClientNote>> listNotes(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.listNotes(id));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<?> addNote(@PathVariable Long id, @RequestBody ClientNote body, Principal principal) {
        try {
            String author = principal != null ? principal.getName() : null;
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(clientService.addNote(id, body.getKind(), body.getText(), author));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id, @PathVariable Long noteId) {
        clientService.deleteNote(id, noteId);
        return ResponseEntity.noContent().build();
    }

    // ── Устройства ─────────────────────────────────────────────

    @GetMapping("/{id}/devices")
    public ResponseEntity<List<ClientDevice>> listDevices(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.listDevices(id));
    }

    @PostMapping("/{id}/devices")
    public ResponseEntity<?> addDevice(@PathVariable Long id, @RequestBody ClientDevice body) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(clientService.addDevice(id, body));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/devices/{deviceId}")
    public ResponseEntity<?> updateDevice(@PathVariable Long id, @PathVariable Long deviceId,
                                          @RequestBody ClientDevice body) {
        try {
            return ResponseEntity.ok(clientService.updateDevice(id, deviceId, body));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/devices/{deviceId}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id, @PathVariable Long deviceId) {
        clientService.deleteDevice(id, deviceId);
        return ResponseEntity.noContent().build();
    }

    // ── Фото (в т.ч. из мобильного клиента) ────────────────────

    @GetMapping("/{id}/photos")
    public ResponseEntity<?> listPhotos(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.listPhotos(id));
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addPhotos(@PathVariable Long id,
                                       @RequestParam("files") MultipartFile[] files,
                                       @RequestParam(value = "caption", required = false) String caption,
                                       @RequestParam(value = "deviceId", required = false) Long deviceId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(clientService.addPhotos(id, files, caption, deviceId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/photos/{photoId}/raw")
    public ResponseEntity<byte[]> rawPhoto(@PathVariable Long id, @PathVariable Long photoId) {
        return clientService.getPhotoData(id, photoId)
                .map(data -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(
                                data.contentType() == null ? "application/octet-stream" : data.contentType()))
                        .body(data.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        clientService.deletePhoto(id, photoId);
        return ResponseEntity.noContent().build();
    }
}
