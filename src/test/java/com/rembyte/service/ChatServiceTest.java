package com.rembyte.service;

import com.rembyte.model.ChatConversation;
import com.rembyte.model.ChatMessage;
import com.rembyte.model.ChatWidgetSite;
import com.rembyte.repository.AppUserRepository;
import com.rembyte.repository.ChatConversationRepository;
import com.rembyte.repository.ChatMessageRepository;
import com.rembyte.repository.ChatWidgetSiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Test
    void createConversation_shouldIncrementUnreadForOperatorWhenVisitorSendsFirstMessage() {
        ChatConversationRepository conversationRepository = mock(ChatConversationRepository.class);
        ChatMessageRepository messageRepository = mock(ChatMessageRepository.class);
        ChatWidgetSiteRepository widgetSiteRepository = mock(ChatWidgetSiteRepository.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);

        ChatWidgetSite site = new ChatWidgetSite();
        site.setId(1L);
        site.setSiteKey(ChatService.DEFAULT_SITE_KEY);
        site.setDisplayName("Основной сайт");
        site.setWidgetTitle("FixByte чат");
        site.setWelcomeMessage("Здравствуйте");
        site.setEnabled(true);

        when(widgetSiteRepository.findBySiteKey(ChatService.DEFAULT_SITE_KEY)).thenReturn(Optional.of(site));

        AtomicLong conversationId = new AtomicLong(100);
        when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(conversationId.getAndIncrement());
            }
            return conversation;
        });

        AtomicLong messageId = new AtomicLong(500);
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(messageId.getAndIncrement());
            }
            return message;
        });
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any(Long.class))).thenAnswer(invocation -> List.of());

        ChatService chatService = new ChatService(conversationRepository, messageRepository, widgetSiteRepository, appUserRepository);

        ChatService.ConversationDetail detail = chatService.createConversation(new ChatService.CreateConversationRequest(
                ChatService.DEFAULT_SITE_KEY,
                "Иван",
                "+79990000000",
                "ivan@test.ru",
                "https://fix-byte.ru/repair",
                "https://fix-byte.ru",
                "JUnit",
                "Здравствуйте, нужен ремонт ноутбука"
        ));

        assertNotNull(detail);
        assertNotNull(detail.conversation());
        assertEquals(1, detail.conversation().unreadForOperator());
        verify(messageRepository, atLeastOnce()).save(any(ChatMessage.class));
        verify(conversationRepository, atLeast(2)).save(any(ChatConversation.class));
    }

    @Test
    void getWidgetSiteForPublic_shouldRejectDisallowedOrigin() {
        ChatConversationRepository conversationRepository = mock(ChatConversationRepository.class);
        ChatMessageRepository messageRepository = mock(ChatMessageRepository.class);
        ChatWidgetSiteRepository widgetSiteRepository = mock(ChatWidgetSiteRepository.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);

        ChatWidgetSite site = new ChatWidgetSite();
        site.setId(1L);
        site.setSiteKey(ChatService.DEFAULT_SITE_KEY);
        site.setDisplayName("Основной сайт");
        site.setWidgetTitle("FixByte чат");
        site.setWelcomeMessage("Здравствуйте");
        site.setAllowedOriginsText("https://fix-byte.ru\nhttps://www.fix-byte.ru");
        site.setEnabled(true);

        when(widgetSiteRepository.findBySiteKey(ChatService.DEFAULT_SITE_KEY)).thenReturn(Optional.of(site));

        ChatService chatService = new ChatService(conversationRepository, messageRepository, widgetSiteRepository, appUserRepository);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> chatService.getWidgetSiteForPublic(ChatService.DEFAULT_SITE_KEY, "https://evil.example"));

        assertTrue(ex.getMessage().contains("не разрешен"));
    }
}

