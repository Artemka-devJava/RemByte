package com.rembyte.controller;

import com.rembyte.model.ChatConversationStatus;
import com.rembyte.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class ChatOperatorController {

    private final ChatService chatService;

    public ChatOperatorController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatService.ConversationListItem>> getConversations() {
        return ResponseEntity.ok(chatService.getConversations());
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<?> getConversation(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(chatService.getConversation(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(chatService.getMessages(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long id,
                                         @RequestBody ChatService.SendMessageRequest request,
                                         Authentication authentication) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(chatService.addOperatorMessage(id, authentication == null ? null : authentication.getName(), request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/conversations/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam ChatConversationStatus status) {
        try {
            return ResponseEntity.ok(chatService.updateConversationStatus(id, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        try {
            chatService.markOperatorRead(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<ChatService.ChatSummaryView> getSummary() {
        return ResponseEntity.ok(chatService.getSummary());
    }

    @GetMapping("/widget-site")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChatService.WidgetSiteView> getWidgetSite() {
        return ResponseEntity.ok(chatService.getWidgetSiteForAdmin());
    }

    @PutMapping("/widget-site")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveWidgetSite(@RequestBody ChatService.WidgetSiteUpdateRequest request) {
        try {
            return ResponseEntity.ok(chatService.saveWidgetSite(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

