package com.rembyte.controller;

import com.rembyte.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public/chat")
@CrossOrigin(origins = "*")
public class PublicChatController {

    private final ChatService chatService;

    public PublicChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/site/{siteKey}")
    public ResponseEntity<?> getSite(@PathVariable String siteKey,
                                     @RequestParam(required = false) String parentOrigin) {
        try {
            return ResponseEntity.ok(chatService.getWidgetSiteForPublic(siteKey, parentOrigin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(@RequestBody ChatService.CreateConversationRequest request,
                                                HttpServletRequest httpServletRequest) {
        try {
            ChatService.CreateConversationRequest payload = new ChatService.CreateConversationRequest(
                    request.siteKey(),
                    request.visitorName(),
                    request.visitorPhone(),
                    request.visitorEmail(),
                    request.pageUrl(),
                    request.parentOrigin(),
                    httpServletRequest.getHeader("User-Agent"),
                    request.message()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createConversation(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conversations/{publicToken}")
    public ResponseEntity<?> getConversation(@PathVariable String publicToken) {
        try {
            return ResponseEntity.ok(chatService.getConversationByToken(publicToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conversations/{publicToken}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String publicToken) {
        try {
            return ResponseEntity.ok(chatService.getMessagesByToken(publicToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/conversations/{publicToken}/messages")
    public ResponseEntity<?> sendVisitorMessage(@PathVariable String publicToken,
                                                @RequestBody ChatService.SendMessageRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(chatService.addVisitorMessage(publicToken, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/conversations/{publicToken}/read")
    public ResponseEntity<?> markRead(@PathVariable String publicToken) {
        try {
            chatService.markVisitorRead(publicToken);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
