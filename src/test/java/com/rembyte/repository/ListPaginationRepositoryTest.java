package com.rembyte.repository;

import com.rembyte.model.Client;
import com.rembyte.model.Order;
import com.rembyte.service.OrderListItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет постраничные JPQL-запросы (Order/Client) на реальной БД (H2), а не
 * на моках — сюда, например, попала бы ошибка приоритета AND/OR в WHERE
 * (найдена и исправлена при разработке {@link ClientRepository#searchPage}).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListPaginationRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ClientRepository clientRepository;

    private Client persistClient(String name, String phone, boolean archived) {
        Client c = new Client(name, phone);
        if (archived) c.setArchivedAt(LocalDateTime.now());
        return em.persistFlushFind(c);
    }

    private Order persistOrder(String number, Client client, String status, String device) {
        Order o = new Order(number, client);
        o.setStatus(status);
        o.setDeviceDescription(device);
        o.setTotalPrice(1000.0);
        o.setPaidAmount(500.0);
        return em.persistFlushFind(o);
    }

    // ── Client.searchPage: приоритет AND/OR в mode+q ──────────

    @Test
    void clientSearchPage_activeMode_stillAppliesQueryFilter() {
        persistClient("Иван Петров", "+79990000001", false);
        persistClient("Сергей Иванов", "+79990000002", false);
        persistClient("Мария Сидорова", "+79990000003", true); // архивная — не должна попасть

        Page<Client> page = clientRepository.searchPage("active", "иван", PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Client::getName).containsExactlyInAnyOrder("Иван Петров", "Сергей Иванов");
    }

    @Test
    void clientSearchPage_archivedMode_onlyReturnsArchived() {
        persistClient("Активный", "+79990000011", false);
        Client archived = persistClient("Архивный", "+79990000012", true);

        Page<Client> page = clientRepository.searchPage("archived", "", PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Client::getId).containsExactly(archived.getId());
    }

    @Test
    void clientSearchPage_allMode_ignoresArchivedFlagButStillFilters() {
        persistClient("Ольга Кузнецова", "+79990000021", false);
        persistClient("Пётр Кузнецов", "+79990000022", true);
        persistClient("Не совпадает", "+79990000023", false);

        Page<Client> page = clientRepository.searchPage("all", "кузнец", PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Client::getName)
                .containsExactlyInAnyOrder("Ольга Кузнецова", "Пётр Кузнецов");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void clientSearchPage_respectsPageSize() {
        for (int i = 0; i < 5; i++) persistClient("Клиент " + i, "+7999000" + (1000 + i), false);

        Page<Client> page = clientRepository.searchPage("active", "", PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    // ── Order.searchOrders: статус + поиск + без коллекции lines ──

    @Test
    void orderSearchOrders_filtersByStatusAndSearchText() {
        Client client = persistClient("Алексей Смирнов", "+79990000031", false);
        persistOrder("ФБ-100", client, "NEW", "Ноутбук");
        persistOrder("ФБ-101", client, "COMPLETED", "Телефон");

        Page<OrderListItem> byStatus = orderRepository.searchOrders("COMPLETED", "", PageRequest.of(0, 20));
        assertThat(byStatus.getContent()).extracting(OrderListItem::orderNumber).containsExactly("ФБ-101");

        Page<OrderListItem> byClientName = orderRepository.searchOrders("", "смирнов", PageRequest.of(0, 20));
        assertThat(byClientName.getContent()).hasSize(2);

        Page<OrderListItem> byOrderNumber = orderRepository.searchOrders("", "фб-100", PageRequest.of(0, 20));
        assertThat(byOrderNumber.getContent()).extracting(OrderListItem::orderNumber).containsExactly("ФБ-100");
    }

    @Test
    void orderSearchOrders_projectionCarriesClientAndMoneyFields() {
        Client client = persistClient("Дарья Волкова", "+79990000041", false);
        persistOrder("ФБ-200", client, "NEW", "ПК");

        OrderListItem item = orderRepository.searchOrders("", "", PageRequest.of(0, 20)).getContent().get(0);

        assertThat(item.clientId()).isEqualTo(client.getId());
        assertThat(item.clientName()).isEqualTo("Дарья Волкова");
        assertThat(item.totalPrice()).isEqualTo(1000.0);
        assertThat(item.paidAmount()).isEqualTo(500.0);
    }
}
