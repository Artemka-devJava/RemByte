package com.rembyte.service;

import com.rembyte.model.KanbanBoard;
import com.rembyte.model.KanbanCard;
import com.rembyte.model.KanbanCardAttachment;
import com.rembyte.model.KanbanColumn;
import com.rembyte.repository.KanbanBoardRepository;
import com.rembyte.repository.KanbanCardAttachmentRepository;
import com.rembyte.repository.KanbanCardRepository;
import com.rembyte.repository.KanbanColumnRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KanbanService {

    private static final List<String> DEFAULT_COLUMNS = List.of("К выполнению", "В работе", "Ожидает", "Готово");

    @Value("${fixbyte.upload.max-files-per-request:10}")
    private int maxFilesPerRequest;

    @Value("${fixbyte.upload.max-image-size:15MB}")
    private DataSize maxImageSize;

    @Value("${fixbyte.upload.max-file-size:20MB}")
    private DataSize maxTextFileSize;

    private final KanbanBoardRepository boardRepository;
    private final KanbanColumnRepository columnRepository;
    private final KanbanCardRepository cardRepository;
    private final KanbanCardAttachmentRepository attachmentRepository;

    public KanbanService(KanbanBoardRepository boardRepository,
                         KanbanColumnRepository columnRepository,
                         KanbanCardRepository cardRepository,
                         KanbanCardAttachmentRepository attachmentRepository) {
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional
    public void initializeDefaultBoardForUser(String username) {
        ensureDefaultBoardForUser(username);
    }

    @Transactional
    public List<BoardSummaryView> getBoards(String username) {
        String owner = normalizeUsername(username);
        ensureDefaultBoardForUser(owner);
        return boardRepository.findByOwnerUsernameOrderByUpdatedAtDescIdAsc(owner).stream()
                .map(board -> new BoardSummaryView(board.getId(), board.getName(), board.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public BoardSummaryView createBoard(String username, CreateBoardRequest request) {
        String owner = normalizeUsername(username);
        String name = sanitize(request.name());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Название доски не может быть пустым");
        }

        KanbanBoard board = new KanbanBoard(owner, name);
        board.setCreatedAt(LocalDateTime.now());
        board.setUpdatedAt(LocalDateTime.now());
        board = boardRepository.save(board);
        createDefaultColumns(board);

        return new BoardSummaryView(board.getId(), board.getName(), board.getUpdatedAt());
    }

    @Transactional
    public BoardSummaryView renameBoard(String username, Long boardId, RenameBoardRequest request) {
        KanbanBoard board = requireBoard(boardId, username);
        String name = sanitize(request.name());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Название доски не может быть пустым");
        }

        board.setName(name);
        board.setUpdatedAt(LocalDateTime.now());
        board = boardRepository.save(board);
        return new BoardSummaryView(board.getId(), board.getName(), board.getUpdatedAt());
    }

    @Transactional
    public BoardView getBoard(String username, Long boardId) {
        KanbanBoard board = resolveBoardForUser(username, boardId);
        List<KanbanColumn> columns = columnRepository.findByBoardIdOrderByPositionAscIdAsc(board.getId());
        List<Long> columnIds = columns.stream().map(KanbanColumn::getId).toList();

        Map<Long, List<CardView>> cardsByColumn = columnIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> cardRepository.findByColumnIdOrderByPositionAscIdAsc(id).stream()
                                .map(this::toCardView)
                                .collect(Collectors.toCollection(ArrayList::new))
                ));

        List<ColumnView> columnViews = columns.stream()
                .map(column -> new ColumnView(
                        column.getId(),
                        column.getName(),
                        nullSafe(column.getPosition()),
                        cardsByColumn.getOrDefault(column.getId(), List.of())
                ))
                .toList();

        return new BoardView(board.getId(), board.getName(), columnViews);
    }

    @Transactional
    public ColumnView renameColumn(String username, Long columnId, RenameColumnRequest request) {
        KanbanColumn column = requireColumn(columnId);
        ensureBoardAccess(column.getBoard(), username);

        String name = sanitize(request.name());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Название колонки не может быть пустым");
        }

        column.setName(name);
        column.setUpdatedAt(LocalDateTime.now());
        column = columnRepository.save(column);

        touchBoard(column.getBoard());
        return new ColumnView(column.getId(), column.getName(), nullSafe(column.getPosition()), List.of());
    }

    @Transactional
    public ColumnView createColumn(String username, Long boardId, CreateColumnRequest request) {
        KanbanBoard board = requireBoard(boardId, username);

        String name = sanitize(request.name());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Название колонки не может быть пустым");
        }

        int nextPosition = columnRepository.findByBoardIdOrderByPositionAscIdAsc(board.getId()).size();

        KanbanColumn column = new KanbanColumn(board, name, nextPosition);
        column.setCreatedAt(LocalDateTime.now());
        column.setUpdatedAt(LocalDateTime.now());
        column = columnRepository.save(column);

        touchBoard(board);
        return new ColumnView(column.getId(), column.getName(), nullSafe(column.getPosition()), List.of());
    }

    @Transactional
    public void deleteColumn(String username, Long columnId) {
        KanbanColumn column = requireColumn(columnId);
        KanbanBoard board = column.getBoard();
        ensureBoardAccess(board, username);

        List<KanbanColumn> columns = columnRepository.findByBoardIdOrderByPositionAscIdAsc(board.getId());
        if (columns.size() <= 1) {
            throw new IllegalArgumentException("Нельзя удалить последнюю колонку на доске");
        }

        int removedPosition = nullSafe(column.getPosition());

        List<KanbanCard> cards = cardRepository.findByColumnIdOrderByPositionAscIdAsc(columnId);
        if (!cards.isEmpty()) {
            cardRepository.deleteAll(cards);
        }
        columnRepository.delete(column);

        for (KanbanColumn current : columns) {
            if (Objects.equals(current.getId(), column.getId())) {
                continue;
            }
            if (nullSafe(current.getPosition()) > removedPosition) {
                current.setPosition(nullSafe(current.getPosition()) - 1);
                current.setUpdatedAt(LocalDateTime.now());
                columnRepository.save(current);
            }
        }

        touchBoard(board);
    }

    @Transactional
    public CardView createCard(String username, Long columnId, CreateCardRequest request) {
        KanbanColumn column = requireColumn(columnId);
        ensureBoardAccess(column.getBoard(), username);

        String title = sanitize(request.title());
        if (title.isBlank()) {
            throw new IllegalArgumentException("Название карточки не может быть пустым");
        }

        KanbanCard card = new KanbanCard();
        card.setColumn(column);
        card.setTitle(title);
        card.setDescription(sanitize(request.description()));
        card.setPosition((int) cardRepository.countByColumnId(column.getId()));
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());

        card = cardRepository.save(card);
        touchBoard(column.getBoard());
        return toCardView(card);
    }

    @Transactional
    public CardView updateCard(String username, Long cardId, UpdateCardRequest request) {
        KanbanCard card = requireCard(cardId);
        ensureBoardAccess(card.getColumn().getBoard(), username);

        String title = sanitize(request.title());
        if (title.isBlank()) {
            throw new IllegalArgumentException("Название карточки не может быть пустым");
        }

        card.setTitle(title);
        card.setDescription(sanitize(request.description()));
        card.setUpdatedAt(LocalDateTime.now());
        card = cardRepository.save(card);

        touchBoard(card.getColumn().getBoard());
        return toCardView(card);
    }

    @Transactional
    public CardView moveCard(String username, Long cardId, MoveCardRequest request) {
        KanbanCard card = requireCard(cardId);
        ensureBoardAccess(card.getColumn().getBoard(), username);
        final Long movingCardId = card.getId();

        KanbanColumn targetColumn = requireColumn(request.columnId());
        ensureBoardAccess(targetColumn.getBoard(), username);

        Integer sourcePosition = nullSafe(card.getPosition());
        KanbanColumn sourceColumn = card.getColumn();
        int targetPosition = Math.max(0, nullSafe(request.position()));

        List<KanbanCard> sourceCards = cardRepository.findByColumnIdOrderByPositionAscIdAsc(sourceColumn.getId());
        for (KanbanCard sourceCard : sourceCards) {
            if (Objects.equals(sourceCard.getId(), card.getId())) {
                continue;
            }
            if (nullSafe(sourceCard.getPosition()) > sourcePosition) {
                sourceCard.setPosition(nullSafe(sourceCard.getPosition()) - 1);
                sourceCard.setUpdatedAt(LocalDateTime.now());
                cardRepository.save(sourceCard);
            }
        }

        List<KanbanCard> targetCards = cardRepository.findByColumnIdOrderByPositionAscIdAsc(targetColumn.getId());
        if (Objects.equals(sourceColumn.getId(), targetColumn.getId())) {
            targetCards = targetCards.stream()
                    .filter(existing -> !Objects.equals(existing.getId(), movingCardId))
                    .toList();
        }

        int boundedPosition = Math.min(targetPosition, targetCards.size());
        for (int i = 0; i < targetCards.size(); i++) {
            KanbanCard targetCard = targetCards.get(i);
            int newPosition = i >= boundedPosition ? i + 1 : i;
            if (nullSafe(targetCard.getPosition()) != newPosition) {
                targetCard.setPosition(newPosition);
                targetCard.setUpdatedAt(LocalDateTime.now());
                cardRepository.save(targetCard);
            }
        }

        card.setColumn(targetColumn);
        card.setPosition(boundedPosition);
        card.setUpdatedAt(LocalDateTime.now());
        card = cardRepository.save(card);

        touchBoard(targetColumn.getBoard());
        return toCardView(card);
    }

    @Transactional
    public void deleteCard(String username, Long cardId) {
        KanbanCard card = requireCard(cardId);
        ensureBoardAccess(card.getColumn().getBoard(), username);

        Long columnId = card.getColumn().getId();
        int removedPosition = nullSafe(card.getPosition());
        KanbanBoard board = card.getColumn().getBoard();

        cardRepository.delete(card);

        List<KanbanCard> cards = cardRepository.findByColumnIdOrderByPositionAscIdAsc(columnId);
        for (KanbanCard current : cards) {
            if (nullSafe(current.getPosition()) > removedPosition) {
                current.setPosition(nullSafe(current.getPosition()) - 1);
                current.setUpdatedAt(LocalDateTime.now());
                cardRepository.save(current);
            }
        }

        touchBoard(board);
    }

    @Transactional
    public List<AttachmentView> uploadCardAttachments(String username, Long cardId, MultipartFile[] files) {
        KanbanCard card = requireCard(cardId);
        ensureBoardAccess(card.getColumn().getBoard(), username);

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Файлы не переданы");
        }
        if (files.length > maxFilesPerRequest) {
            throw new IllegalArgumentException("Слишком много файлов за один запрос");
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String contentType = normalizeContentType(file.getContentType());
            String extension = resolveExtension(file.getOriginalFilename(), contentType);
            boolean isImage = contentType.startsWith("image/") && isAllowedImageExtension(extension);
            boolean isText = isTextAttachment(contentType, extension);

            if (!isImage && !isText) {
                throw new IllegalArgumentException("Допустимы только фото и текстовые файлы");
            }

            if (isImage && file.getSize() > maxImageSize.toBytes()) {
                throw new IllegalArgumentException("Слишком большой файл изображения");
            }
            if (isText && file.getSize() > maxTextFileSize.toBytes()) {
                throw new IllegalArgumentException("Слишком большой текстовый файл");
            }

            try {
                KanbanCardAttachment attachment = new KanbanCardAttachment();
                attachment.setCard(card);
                attachment.setStoredName(System.currentTimeMillis() + "_" + UUID.randomUUID() + extension);
                attachment.setOriginalName(sanitizeOriginalName(file.getOriginalFilename(), attachment.getStoredName()));
                attachment.setContentType(contentType.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType);
                attachment.setAttachmentType(isImage ? KanbanCardAttachment.AttachmentType.IMAGE : KanbanCardAttachment.AttachmentType.TEXT);
                attachment.setContent(file.getBytes());
                attachment.setCreatedAt(LocalDateTime.now());
                attachmentRepository.save(attachment);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка сохранения вложения", e);
            }
        }

        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);
        touchBoard(card.getColumn().getBoard());

        return getCardAttachments(username, cardId);
    }

    @Transactional(readOnly = true)
    public List<AttachmentView> getCardAttachments(String username, Long cardId) {
        KanbanCard card = requireCard(cardId);
        ensureBoardAccess(card.getColumn().getBoard(), username);

        return attachmentRepository.findByCardIdOrderByCreatedAtAscIdAsc(cardId).stream()
                .map(this::toAttachmentView)
                .toList();
    }

    @Transactional
    public void deleteCardAttachment(String username, Long cardId, String attachmentUrl) {
        KanbanCard card = requireCard(cardId);
        ensureBoardAccess(card.getColumn().getBoard(), username);

        String expectedPrefix = "/uploads/kanban/" + cardId + "/";
        if (attachmentUrl == null || !attachmentUrl.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Некорректный путь вложения");
        }

        String storedName = attachmentUrl.substring(expectedPrefix.length());
        if (storedName.contains("/") || storedName.contains("\\") || storedName.contains("..")) {
            throw new IllegalArgumentException("Некорректное имя вложения");
        }

        long deleted = attachmentRepository.deleteByCardIdAndStoredName(cardId, storedName);
        if (deleted == 0) {
            throw new IllegalArgumentException("Вложение не найдено");
        }

        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);
        touchBoard(card.getColumn().getBoard());
    }

    @Transactional(readOnly = true)
    public StoredAttachment loadCardAttachment(String username, Long cardId, String storedName) {
        KanbanCard card = requireCard(cardId);
        ensureBoardAccess(card.getColumn().getBoard(), username);

        KanbanCardAttachment attachment = attachmentRepository.findByCardIdAndStoredName(cardId, storedName)
                .orElseThrow(() -> new IllegalArgumentException("Вложение не найдено"));

        return new StoredAttachment(attachment.getContent(), attachment.getContentType(), attachment.getOriginalName());
    }

    private void touchBoard(KanbanBoard board) {
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);
    }

    private KanbanBoard resolveBoardForUser(String username, Long boardId) {
        String owner = normalizeUsername(username);
        ensureDefaultBoardForUser(owner);

        if (boardId == null) {
            return boardRepository.findFirstByOwnerUsernameOrderByUpdatedAtDescIdAsc(owner)
                    .orElseThrow(() -> new IllegalStateException("Доска не найдена"));
        }
        return requireBoard(boardId, owner);
    }

    private KanbanBoard requireBoard(Long boardId, String username) {
        return boardRepository.findByIdAndOwnerUsername(boardId, normalizeUsername(username))
                .orElseThrow(() -> new IllegalArgumentException("Доска не найдена"));
    }

    private KanbanColumn requireColumn(Long id) {
        return columnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Колонка не найдена"));
    }

    private KanbanCard requireCard(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Карточка не найдена"));
    }

    private void ensureBoardAccess(KanbanBoard board, String username) {
        String owner = normalizeUsername(username);
        if (!owner.equals(board.getOwnerUsername())) {
            throw new IllegalArgumentException("Нет доступа к доске");
        }
    }

    private void ensureDefaultBoardForUser(String username) {
        String owner = normalizeUsername(username);
        if (boardRepository.findFirstByOwnerUsernameOrderByUpdatedAtDescIdAsc(owner).isPresent()) {
            return;
        }

        KanbanBoard board = new KanbanBoard(owner, "Моя доска");
        board.setCreatedAt(LocalDateTime.now());
        board.setUpdatedAt(LocalDateTime.now());
        board = boardRepository.save(board);
        createDefaultColumns(board);
    }

    private void createDefaultColumns(KanbanBoard board) {
        for (int i = 0; i < DEFAULT_COLUMNS.size(); i++) {
            KanbanColumn column = new KanbanColumn(board, DEFAULT_COLUMNS.get(i), i);
            column.setCreatedAt(LocalDateTime.now());
            column.setUpdatedAt(LocalDateTime.now());
            columnRepository.save(column);
        }
    }

    private CardView toCardView(KanbanCard card) {
        List<AttachmentView> attachments = attachmentRepository.findByCardIdOrderByCreatedAtAscIdAsc(card.getId()).stream()
                .map(this::toAttachmentView)
                .toList();

        String preview = attachments.stream()
                .filter(att -> "IMAGE".equals(att.type()))
                .map(AttachmentView::url)
                .findFirst()
                .orElse(null);

        return new CardView(
                card.getId(),
                card.getColumn().getId(),
                card.getTitle(),
                card.getDescription(),
                nullSafe(card.getPosition()),
                preview,
                attachments,
                card.getUpdatedAt()
        );
    }

    private AttachmentView toAttachmentView(KanbanCardAttachment attachment) {
        return new AttachmentView(
                "/uploads/kanban/" + attachment.getCard().getId() + "/" + attachment.getStoredName(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getAttachmentType().name()
        );
    }

    private String normalizeUsername(String username) {
        String value = sanitize(username);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Пользователь не определен");
        }
        return value;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
                if (ext.matches("\\.[a-z0-9]{1,10}")) {
                    return ext;
                }
            }
        }

        if (contentType.contains("jpeg")) return ".jpg";
        if (contentType.contains("png")) return ".png";
        if (contentType.contains("webp")) return ".webp";
        if (contentType.contains("gif")) return ".gif";
        if (contentType.contains("plain")) return ".txt";
        return ".bin";
    }

    private boolean isAllowedImageExtension(String ext) {
        return Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext);
    }

    private boolean isTextAttachment(String contentType, String ext) {
        if (contentType.startsWith("text/")) return true;
        return Set.of(".txt", ".md", ".csv", ".log").contains(ext);
    }

    private String sanitizeOriginalName(String originalName, String fallback) {
        if (originalName == null || originalName.isBlank()) {
            return fallback;
        }
        return originalName.replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    public record BoardSummaryView(Long id, String name, LocalDateTime updatedAt) {}

    public record BoardView(Long id, String name, List<ColumnView> columns) {}

    public record ColumnView(Long id, String name, int position, List<CardView> cards) {}

    public record CardView(Long id,
                           Long columnId,
                           String title,
                           String description,
                           int position,
                           String previewImageUrl,
                           List<AttachmentView> attachments,
                           LocalDateTime updatedAt) {}

    public record AttachmentView(String url, String originalName, String contentType, String type) {}

    public record StoredAttachment(byte[] content, String contentType, String originalName) {}

    public record CreateBoardRequest(String name) {}

    public record RenameBoardRequest(String name) {}

    public record RenameColumnRequest(String name) {}

    public record CreateColumnRequest(String name) {}

    public record CreateCardRequest(String title, String description) {}

    public record UpdateCardRequest(String title, String description) {}

    public record MoveCardRequest(Long columnId, Integer position) {}
}
