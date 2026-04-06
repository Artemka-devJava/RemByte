package com.rembyte.controller;

import com.rembyte.service.KanbanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kanban")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class KanbanController {

    private final KanbanService kanbanService;

    public KanbanController(KanbanService kanbanService) {
        this.kanbanService = kanbanService;
    }

    @GetMapping("/boards")
    public ResponseEntity<List<KanbanService.BoardSummaryView>> getBoards(Authentication authentication) {
        return ResponseEntity.ok(kanbanService.getBoards(authentication.getName()));
    }

    @PostMapping("/boards")
    public ResponseEntity<?> createBoard(@RequestBody KanbanService.CreateBoardRequest request,
                                         Authentication authentication) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(kanbanService.createBoard(authentication.getName(), request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/boards/{boardId}")
    public ResponseEntity<?> renameBoard(@PathVariable Long boardId,
                                         @RequestBody KanbanService.RenameBoardRequest request,
                                         Authentication authentication) {
        try {
            return ResponseEntity.ok(kanbanService.renameBoard(authentication.getName(), boardId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/board")
    public ResponseEntity<?> getBoard(@RequestParam(required = false) Long boardId,
                                      Authentication authentication) {
        try {
            return ResponseEntity.ok(kanbanService.getBoard(authentication.getName(), boardId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/columns/{columnId}")
    public ResponseEntity<?> renameColumn(@PathVariable Long columnId,
                                          @RequestBody KanbanService.RenameColumnRequest request,
                                          Authentication authentication) {
        try {
            return ResponseEntity.ok(kanbanService.renameColumn(authentication.getName(), columnId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/boards/{boardId}/columns")
    public ResponseEntity<?> createColumn(@PathVariable Long boardId,
                                          @RequestBody KanbanService.CreateColumnRequest request,
                                          Authentication authentication) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(kanbanService.createColumn(authentication.getName(), boardId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/columns/{columnId}")
    public ResponseEntity<?> deleteColumn(@PathVariable Long columnId,
                                          Authentication authentication) {
        try {
            kanbanService.deleteColumn(authentication.getName(), columnId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/columns/{columnId}/cards")
    public ResponseEntity<?> createCard(@PathVariable Long columnId,
                                        @RequestBody KanbanService.CreateCardRequest request,
                                        Authentication authentication) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(kanbanService.createCard(authentication.getName(), columnId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/cards/{cardId}")
    public ResponseEntity<?> updateCard(@PathVariable Long cardId,
                                        @RequestBody KanbanService.UpdateCardRequest request,
                                        Authentication authentication) {
        try {
            return ResponseEntity.ok(kanbanService.updateCard(authentication.getName(), cardId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/cards/{cardId}/move")
    public ResponseEntity<?> moveCard(@PathVariable Long cardId,
                                      @RequestBody KanbanService.MoveCardRequest request,
                                      Authentication authentication) {
        try {
            return ResponseEntity.ok(kanbanService.moveCard(authentication.getName(), cardId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<?> deleteCard(@PathVariable Long cardId,
                                        Authentication authentication) {
        try {
            kanbanService.deleteCard(authentication.getName(), cardId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cards/{cardId}/attachments")
    public ResponseEntity<?> uploadAttachments(@PathVariable Long cardId,
                                               @RequestParam("files") MultipartFile[] files,
                                               Authentication authentication) {
        try {
            return ResponseEntity.ok(kanbanService.uploadCardAttachments(authentication.getName(), cardId, files));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cards/{cardId}/attachments")
    public ResponseEntity<?> getAttachments(@PathVariable Long cardId,
                                            Authentication authentication) {
        try {
            return ResponseEntity.ok(kanbanService.getCardAttachments(authentication.getName(), cardId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/cards/{cardId}/attachments")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long cardId,
                                              @RequestParam("url") String url,
                                              Authentication authentication) {
        try {
            kanbanService.deleteCardAttachment(authentication.getName(), cardId, url);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
