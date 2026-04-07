package com.rembyte.repository;

import com.rembyte.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    void deleteByConversationId(Long conversationId);
}

