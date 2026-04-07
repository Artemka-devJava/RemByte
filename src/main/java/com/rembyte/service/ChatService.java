package com.rembyte.service;

import com.rembyte.model.*;
import com.rembyte.repository.AppUserRepository;
import com.rembyte.repository.ChatConversationRepository;
import com.rembyte.repository.ChatMessageRepository;
import com.rembyte.repository.ChatWidgetSiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.net.URI;

@Service
public class ChatService {

    public static final String DEFAULT_SITE_KEY = "main-site";
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatWidgetSiteRepository widgetSiteRepository;
    private final AppUserRepository appUserRepository;

    public ChatService(ChatConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       ChatWidgetSiteRepository widgetSiteRepository,
                       AppUserRepository appUserRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.widgetSiteRepository = widgetSiteRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public void initializeDefaultWidgetSite() {
        widgetSiteRepository.findBySiteKey(DEFAULT_SITE_KEY).orElseGet(() -> {
            ChatWidgetSite site = new ChatWidgetSite();
            site.setSiteKey(DEFAULT_SITE_KEY);
            site.setDisplayName("Основной сайт");
            site.setWidgetTitle("FixByte — онлайн чат");
            site.setWelcomeMessage("Здравствуйте! Напишите ваш вопрос, и мы ответим в CRM.");
            site.setAccentColor("#3699d9");
            site.setAllowedOriginsText("");
            site.setEnabled(true);
            site.setCreatedAt(LocalDateTime.now());
            site.setUpdatedAt(LocalDateTime.now());
            return widgetSiteRepository.save(site);
        });
    }

    @Transactional(readOnly = true)
    public WidgetSiteView getWidgetSiteForAdmin() {
        initializeDefaultWidgetSite();
        return toWidgetSiteView(widgetSiteRepository.findBySiteKey(DEFAULT_SITE_KEY).orElseThrow());
    }

    @Transactional(readOnly = true)
    public WidgetSiteView getWidgetSiteForPublic(String siteKey, String parentOrigin) {
        ChatWidgetSite site = getPublicSiteEntity(siteKey, parentOrigin);
        return toWidgetSiteView(site);
    }

    @Transactional
    public WidgetSiteView saveWidgetSite(WidgetSiteUpdateRequest request) {
        initializeDefaultWidgetSite();
        ChatWidgetSite site = widgetSiteRepository.findBySiteKey(DEFAULT_SITE_KEY).orElseThrow();

        String requestedSiteKey = sanitize(request.siteKey());
        if (!requestedSiteKey.isBlank() && !requestedSiteKey.equals(site.getSiteKey())) {
            Optional<ChatWidgetSite> existing = widgetSiteRepository.findBySiteKey(requestedSiteKey);
            if (existing.isPresent() && !Objects.equals(existing.get().getId(), site.getId())) {
                throw new IllegalArgumentException("Такой siteKey уже существует");
            }
            site.setSiteKey(requestedSiteKey);
        }

        site.setDisplayName(defaultIfBlank(request.displayName(), "Основной сайт"));
        site.setWidgetTitle(defaultIfBlank(request.widgetTitle(), "FixByte — онлайн чат"));
        site.setWelcomeMessage(defaultIfBlank(request.welcomeMessage(), "Здравствуйте! Напишите ваш вопрос, и мы ответим в CRM."));
        site.setAccentColor(normalizeColor(request.accentColor()));
        site.setAllowedOriginsText(normalizeOriginsText(request.allowedOriginsText()));
        site.setEnabled(request.enabled());
        site.setUpdatedAt(LocalDateTime.now());
        widgetSiteRepository.save(site);
        return toWidgetSiteView(site);
    }

    @Transactional(readOnly = true)
    public List<ConversationListItem> getConversations() {
        return conversationRepository.findAllByOrderByLastMessageAtDesc().stream()
                .map(this::toConversationListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetail getConversation(Long id) {
        ChatConversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Диалог не найден"));
        return toConversationDetail(conversation);
    }

    @Transactional(readOnly = true)
    public List<MessageView> getMessages(Long conversationId) {
        requireConversation(conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetail getConversationByToken(String publicToken) {
        ChatConversation conversation = conversationRepository.findByPublicToken(publicToken)
                .orElseThrow(() -> new IllegalArgumentException("Диалог не найден"));
        return toConversationDetail(conversation);
    }

    @Transactional(readOnly = true)
    public List<MessageView> getMessagesByToken(String publicToken) {
        ChatConversation conversation = conversationRepository.findByPublicToken(publicToken)
                .orElseThrow(() -> new IllegalArgumentException("Диалог не найден"));
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(this::toMessageView)
                .toList();
    }

    @Transactional
    public ConversationDetail createConversation(CreateConversationRequest request) {
        ChatWidgetSite site = getPublicSiteEntity(request.siteKey(), request.parentOrigin());

        ChatConversation conversation = new ChatConversation();
        conversation.setSiteKey(site.getSiteKey());
        conversation.setVisitorName(sanitize(request.visitorName()));
        conversation.setVisitorPhone(sanitize(request.visitorPhone()));
        conversation.setVisitorEmail(sanitize(request.visitorEmail()));
        conversation.setVisitorPageUrl(sanitize(request.pageUrl()));
        conversation.setVisitorOrigin(sanitize(request.parentOrigin()));
        conversation.setVisitorUserAgent(sanitize(request.userAgent()));
        conversation.setStatus(ChatConversationStatus.OPEN);
        conversation.setUnreadForOperator(0);
        conversation.setUnreadForVisitor(0);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        String initialMessage = sanitizeMessage(request.message());
        if (!initialMessage.isBlank()) {
            appendMessage(conversation, ChatMessageSenderType.VISITOR, visitorSenderName(conversation), initialMessage);
        }

        return toConversationDetail(conversation);
    }

    @Transactional
    public MessageView addVisitorMessage(String publicToken, SendMessageRequest request) {
        ChatConversation conversation = conversationRepository.findByPublicToken(publicToken)
                .orElseThrow(() -> new IllegalArgumentException("Диалог не найден"));
        if (conversation.getStatus() == ChatConversationStatus.CLOSED) {
            conversation.setStatus(ChatConversationStatus.OPEN);
        }
        return appendMessage(conversation, ChatMessageSenderType.VISITOR, visitorSenderName(conversation), request.message());
    }

    @Transactional
    public MessageView addOperatorMessage(Long conversationId, String operatorUsername, SendMessageRequest request) {
        ChatConversation conversation = requireConversation(conversationId);
        if (conversation.getStatus() == ChatConversationStatus.CLOSED) {
            conversation.setStatus(ChatConversationStatus.OPEN);
        }
        if (conversation.getAssignedOperatorUsername() == null || conversation.getAssignedOperatorUsername().isBlank()) {
            conversation.setAssignedOperatorUsername(sanitize(operatorUsername));
        }
        String senderName = resolveOperatorDisplayName(operatorUsername);
        return appendMessage(conversation, ChatMessageSenderType.OPERATOR, senderName, request.message());
    }

    @Transactional
    public ConversationDetail updateConversationStatus(Long conversationId, ChatConversationStatus status) {
        ChatConversation conversation = requireConversation(conversationId);
        conversation.setStatus(status == null ? ChatConversationStatus.OPEN : status);
        conversation.setUpdatedAt(LocalDateTime.now());
        return toConversationDetail(conversationRepository.save(conversation));
    }

    @Transactional
    public void markOperatorRead(Long conversationId) {
        ChatConversation conversation = requireConversation(conversationId);
        conversation.setUnreadForOperator(0);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Transactional
    public void markVisitorRead(String publicToken) {
        ChatConversation conversation = conversationRepository.findByPublicToken(publicToken)
                .orElseThrow(() -> new IllegalArgumentException("Диалог не найден"));
        conversation.setUnreadForVisitor(0);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        ChatConversation conversation = requireConversation(conversationId);
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    @Transactional(readOnly = true)
    public ChatSummaryView getSummary() {
        long unread = conversationRepository.countByUnreadForOperatorGreaterThan(0);
        long open = conversationRepository.findByStatusOrderByLastMessageAtDesc(ChatConversationStatus.OPEN).size();
        long total = conversationRepository.count();
        return new ChatSummaryView(unread, open, total);
    }

    private ChatConversation requireConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Диалог не найден"));
    }

    private ChatWidgetSite getPublicSiteEntity(String siteKey, String parentOrigin) {
        initializeDefaultWidgetSite();
        String normalizedKey = defaultIfBlank(siteKey, DEFAULT_SITE_KEY);
        ChatWidgetSite site = widgetSiteRepository.findBySiteKey(normalizedKey)
                .orElseThrow(() -> new IllegalArgumentException("Конфигурация чата не найдена"));
        if (!site.isEnabled()) {
            throw new IllegalStateException("Чат временно отключен");
        }
        if (!isOriginAllowed(site, parentOrigin)) {
            throw new IllegalStateException("Этот сайт не разрешен для виджета");
        }
        return site;
    }

    private boolean isOriginAllowed(ChatWidgetSite site, String origin) {
        Set<String> allowed = Set.copyOf(parseOrigins(site.getAllowedOriginsText()));
        if (allowed.isEmpty()) {
            return true;
        }
        String normalizedOrigin = normalizeOriginValue(origin);
        if (normalizedOrigin.isBlank()) {
            return false;
        }
        if (allowed.contains("*")) {
            return true;
        }
        return allowed.contains(normalizedOrigin);
    }

    private List<String> parseOrigins(String raw) {
        String normalized = normalizeOriginsText(raw);
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : normalized.split("\\n")) {
            String value = normalizeOriginValue(part);
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    private MessageView appendMessage(ChatConversation conversation,
                                      ChatMessageSenderType senderType,
                                      String senderName,
                                      String messageText) {
        String normalizedMessage = sanitizeMessage(messageText);
        if (normalizedMessage.isBlank()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSenderType(senderType);
        message.setSenderName(defaultIfBlank(senderName, senderType == ChatMessageSenderType.OPERATOR ? "Оператор" : "Клиент"));
        message.setMessage(normalizedMessage);
        message.setCreatedAt(LocalDateTime.now());
        message = messageRepository.save(message);

        conversation.setLastMessageAt(message.getCreatedAt());
        conversation.setUpdatedAt(message.getCreatedAt());
        if (senderType == ChatMessageSenderType.VISITOR) {
            conversation.setUnreadForOperator((conversation.getUnreadForOperator() == null ? 0 : conversation.getUnreadForOperator()) + 1);
        } else if (senderType == ChatMessageSenderType.OPERATOR) {
            conversation.setUnreadForVisitor((conversation.getUnreadForVisitor() == null ? 0 : conversation.getUnreadForVisitor()) + 1);
            conversation.setUnreadForOperator(0);
        }
        conversationRepository.save(conversation);
        return toMessageView(message);
    }

    private String resolveOperatorDisplayName(String username) {
        if (username == null || username.isBlank()) {
            return "Оператор";
        }
        return appUserRepository.findByUsername(username)
                .map(user -> defaultIfBlank(user.getDisplayName(), user.getUsername()))
                .orElse(username);
    }

    private ConversationListItem toConversationListItem(ChatConversation conversation) {
        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        String lastMessagePreview = messages.isEmpty() ? "" : shorten(messages.get(messages.size() - 1).getMessage(), 120);
        return new ConversationListItem(
                conversation.getId(),
                conversation.getPublicToken(),
                conversation.getSiteKey(),
                defaultIfBlank(conversation.getVisitorName(), "Посетитель"),
                conversation.getVisitorPhone(),
                conversation.getVisitorEmail(),
                conversation.getVisitorPageUrl(),
                conversation.getVisitorOrigin(),
                conversation.getStatus(),
                conversation.getAssignedOperatorUsername(),
                nullSafe(conversation.getUnreadForOperator()),
                nullSafe(conversation.getUnreadForVisitor()),
                conversation.getLastMessageAt(),
                lastMessagePreview,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private ConversationDetail toConversationDetail(ChatConversation conversation) {
        return new ConversationDetail(
                toConversationListItem(conversation),
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                        .map(this::toMessageView)
                        .toList()
        );
    }

    private MessageView toMessageView(ChatMessage message) {
        return new MessageView(
                message.getId(),
                message.getSenderType(),
                message.getSenderName(),
                message.getMessage(),
                message.getCreatedAt()
        );
    }

    private WidgetSiteView toWidgetSiteView(ChatWidgetSite site) {
        return new WidgetSiteView(
                site.getSiteKey(),
                site.getDisplayName(),
                site.getWidgetTitle(),
                site.getWelcomeMessage(),
                normalizeColor(site.getAccentColor()),
                defaultIfBlank(site.getAllowedOriginsText(), ""),
                site.isEnabled()
        );
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String sanitizeMessage(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOriginsText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", "\n")
                .lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .distinct()
                .map(String::toLowerCase)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private String normalizeColor(String color) {
        String value = sanitize(color);
        return COLOR_PATTERN.matcher(value).matches() ? value : "#3699d9";
    }

    private String normalizeOriginValue(String value) {
        String raw = sanitize(value);
        if (raw.isBlank()) {
            return "";
        }
        if ("*".equals(raw)) {
            return "*";
        }
        String cleaned = raw.replaceAll("/+$", "");
        try {
            URI uri = URI.create(cleaned);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return cleaned.toLowerCase(Locale.ROOT);
            }
            int port = uri.getPort();
            boolean defaultHttp = "http".equalsIgnoreCase(scheme) && port == 80;
            boolean defaultHttps = "https".equalsIgnoreCase(scheme) && port == 443;
            String portPart = (port == -1 || defaultHttp || defaultHttps) ? "" : ":" + port;
            return (scheme + "://" + host + portPart).toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return cleaned.toLowerCase(Locale.ROOT);
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String normalized = sanitize(value);
        return normalized.isBlank() ? defaultValue : normalized;
    }

    private String visitorSenderName(ChatConversation conversation) {
        return defaultIfBlank(conversation.getVisitorName(), "Посетитель");
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private String shorten(String text, int max) {
        String value = sanitize(text);
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "…";
    }

    public record CreateConversationRequest(
            String siteKey,
            String visitorName,
            String visitorPhone,
            String visitorEmail,
            String pageUrl,
            String parentOrigin,
            String userAgent,
            String message
    ) {}

    public record SendMessageRequest(String message) {}

    public record WidgetSiteUpdateRequest(
            String siteKey,
            String displayName,
            String widgetTitle,
            String welcomeMessage,
            String accentColor,
            String allowedOriginsText,
            boolean enabled
    ) {}

    public record WidgetSiteView(
            String siteKey,
            String displayName,
            String widgetTitle,
            String welcomeMessage,
            String accentColor,
            String allowedOriginsText,
            boolean enabled
    ) {}

    public record MessageView(
            Long id,
            ChatMessageSenderType senderType,
            String senderName,
            String message,
            LocalDateTime createdAt
    ) {}

    public record ConversationListItem(
            Long id,
            String publicToken,
            String siteKey,
            String visitorName,
            String visitorPhone,
            String visitorEmail,
            String visitorPageUrl,
            String visitorOrigin,
            ChatConversationStatus status,
            String assignedOperatorUsername,
            int unreadForOperator,
            int unreadForVisitor,
            LocalDateTime lastMessageAt,
            String lastMessagePreview,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ConversationDetail(
            ConversationListItem conversation,
            List<MessageView> messages
    ) {}

    public record ChatSummaryView(long unreadConversations, long openConversations, long totalConversations) {}
}

