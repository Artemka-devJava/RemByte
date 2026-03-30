package com.rembyte.repository;

import com.rembyte.model.ChatConversation;
import com.rembyte.model.ChatConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Optional<ChatConversation> findByPublicToken(String publicToken);
    List<ChatConversation> findByStatusOrderByLastMessageAtDesc(ChatConversationStatus status);
    List<ChatConversation> findAllByOrderByLastMessageAtDesc();
    long countByUnreadForOperatorGreaterThan(int unreadForOperator);
}

