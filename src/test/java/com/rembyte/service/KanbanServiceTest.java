package com.rembyte.service;

import com.rembyte.repository.KanbanBoardRepository;
import com.rembyte.repository.KanbanCardAttachmentRepository;
import com.rembyte.repository.KanbanCardRepository;
import com.rembyte.repository.KanbanColumnRepository;
import com.rembyte.service.KanbanService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Логика персональных канбан-досок: авто-создание доски по умолчанию,
 * позиции колонок/карточек при вставке-удалении-перемещении, проверка владельца.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KanbanServiceTest {

    @Autowired TestEntityManager em;
    @Autowired KanbanBoardRepository boards;
    @Autowired KanbanColumnRepository columns;
    @Autowired KanbanCardRepository cards;
    @Autowired KanbanCardAttachmentRepository attachments;

    private KanbanService service;

    private static final String USER = "master";
    private static final String OTHER = "operator";

    @BeforeEach
    void setUp() {
        service = new KanbanService(boards, columns, cards, attachments);
    }

    private Long defaultBoardId(String user) {
        return service.getBoards(user).get(0).id();
    }

    // ── доски ───────────────────────────────────────────────

    @Test
    void firstAccessCreatesADefaultBoardWithFourColumns() {
        List<BoardSummaryView> list = service.getBoards(USER);

        assertThat(list).hasSize(1);
        BoardView board = service.getBoard(USER, list.get(0).id());
        assertThat(board.columns()).extracting(ColumnView::name)
                .containsExactly("К выполнению", "В работе", "Ожидает", "Готово");
        assertThat(board.columns()).extracting(ColumnView::position).containsExactly(0, 1, 2, 3);
    }

    @Test
    void createBoard_rejectsBlankName() {
        assertThatThrownBy(() -> service.createBoard(USER, new CreateBoardRequest("   ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renameBoard_ofAnotherUserIsRejected() {
        Long id = defaultBoardId(USER);
        assertThatThrownBy(() -> service.renameBoard(OTHER, id, new RenameBoardRequest("Чужая")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не найдена");
    }

    // ── колонки ─────────────────────────────────────────────

    @Test
    void createColumn_appendsAtTheEnd() {
        Long boardId = defaultBoardId(USER);

        ColumnView added = service.createColumn(USER, boardId, new CreateColumnRequest("Диагностика"));

        assertThat(added.position()).isEqualTo(4);
        assertThat(service.getBoard(USER, boardId).columns()).hasSize(5);
    }

    @Test
    void deleteColumn_reindexesTheRestAndDropsItsCards() {
        Long boardId = defaultBoardId(USER);
        BoardView board = service.getBoard(USER, boardId);
        Long second = board.columns().get(1).id();   // position 1 «В работе»
        service.createCard(USER, second, new CreateCardRequest("Карта в удаляемой", null));

        service.deleteColumn(USER, second);

        List<ColumnView> left = service.getBoard(USER, boardId).columns();
        assertThat(left).extracting(ColumnView::name)
                .containsExactly("К выполнению", "Ожидает", "Готово");
        assertThat(left).extracting(ColumnView::position).containsExactly(0, 1, 2);
        assertThat(cards.count()).isZero();
    }

    @Test
    void deleteColumn_refusesToRemoveTheLastOne() {
        Long boardId = defaultBoardId(USER);
        List<ColumnView> cols = service.getBoard(USER, boardId).columns();
        // удалить 3 из 4 — ок, последнюю нельзя
        service.deleteColumn(USER, cols.get(3).id());
        service.deleteColumn(USER, cols.get(2).id());
        service.deleteColumn(USER, cols.get(1).id());
        Long last = service.getBoard(USER, boardId).columns().get(0).id();

        assertThatThrownBy(() -> service.deleteColumn(USER, last))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("последнюю");
    }

    // ── карточки ────────────────────────────────────────────

    @Test
    void createCard_getsNextPositionInColumn_blankTitleRejected() {
        Long col = service.getBoard(USER, defaultBoardId(USER)).columns().get(0).id();

        assertThat(service.createCard(USER, col, new CreateCardRequest("A", null)).position()).isZero();
        assertThat(service.createCard(USER, col, new CreateCardRequest("B", "desc")).position()).isEqualTo(1);
        assertThatThrownBy(() -> service.createCard(USER, col, new CreateCardRequest("  ", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moveCard_reordersWithinColumn() {
        Long col = service.getBoard(USER, defaultBoardId(USER)).columns().get(0).id();
        var a = service.createCard(USER, col, new CreateCardRequest("A", null));
        service.createCard(USER, col, new CreateCardRequest("B", null));
        service.createCard(USER, col, new CreateCardRequest("C", null));
        flush();

        // A (0) → в конец (позиция 2)
        service.moveCard(USER, a.id(), new MoveCardRequest(col, 2));
        flush();

        List<CardView> ordered = service.getBoard(USER, defaultBoardId(USER)).columns().get(0).cards();
        assertThat(ordered).extracting(CardView::title).containsExactly("B", "C", "A");
        assertThat(ordered).extracting(CardView::position).containsExactly(0, 1, 2);
    }

    @Test
    void moveCard_acrossColumnsKeepsBothSidesContiguous() {
        BoardView board = service.getBoard(USER, defaultBoardId(USER));
        Long src = board.columns().get(0).id();
        Long dst = board.columns().get(1).id();
        var a = service.createCard(USER, src, new CreateCardRequest("A", null));
        service.createCard(USER, src, new CreateCardRequest("B", null));
        service.createCard(USER, dst, new CreateCardRequest("X", null));
        flush();

        service.moveCard(USER, a.id(), new MoveCardRequest(dst, 0));
        flush();

        BoardView after = service.getBoard(USER, defaultBoardId(USER));
        assertThat(after.columns().get(0).cards()).extracting(CardView::title).containsExactly("B");
        assertThat(after.columns().get(0).cards()).extracting(CardView::position).containsExactly(0);
        assertThat(after.columns().get(1).cards()).extracting(CardView::title).containsExactly("A", "X");
        assertThat(after.columns().get(1).cards()).extracting(CardView::position).containsExactly(0, 1);
    }

    @Test
    void deleteCard_reindexesFollowingCards() {
        Long col = service.getBoard(USER, defaultBoardId(USER)).columns().get(0).id();
        service.createCard(USER, col, new CreateCardRequest("A", null));
        var b = service.createCard(USER, col, new CreateCardRequest("B", null));
        service.createCard(USER, col, new CreateCardRequest("C", null));
        flush();

        service.deleteCard(USER, b.id());
        flush();

        List<CardView> left = service.getBoard(USER, defaultBoardId(USER)).columns().get(0).cards();
        assertThat(left).extracting(CardView::title).containsExactly("A", "C");
        assertThat(left).extracting(CardView::position).containsExactly(0, 1);
    }

    // ── доступ ──────────────────────────────────────────────

    @Test
    void anotherUserCannotTouchSomeoneElsesCards() {
        Long col = service.getBoard(USER, defaultBoardId(USER)).columns().get(0).id();

        assertThatThrownBy(() -> service.createCard(OTHER, col, new CreateCardRequest("Взлом", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Нет доступа");
    }

    private void flush() {
        em.flush();
        em.clear();
    }
}
