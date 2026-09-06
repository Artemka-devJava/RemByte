package com.rembyte.model;

import com.rembyte.repository.OrderLineRepository;
import com.rembyte.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет отображение состава заказа на строки {@link OrderLine}:
 * каскад, orphanRemoval при полной замене, пересчёт суммы, удаление позиции.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderLinePersistenceTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    private Client persistClient() {
        Client c = new Client("Иван Петров", "+7 999 000-00-01");
        return em.persistFlushFind(c);
    }

    private RepairService persistService(String name, double price) {
        RepairService s = new RepairService(name, price, "Прочее");
        return em.persistFlushFind(s);
    }

    private OrderLine line(RepairService svc, String name, double unit, int qty) {
        OrderLine l = new OrderLine();
        l.setService(svc);
        l.setName(name);
        l.setUnitPrice(unit);
        l.setQuantity(qty);
        return l;
    }

    @Test
    void savesOrderWithCatalogAndOneOffLines_andComputesTotal() {
        Client client = persistClient();
        RepairService svc = persistService("Диагностика", 500.0);

        Order order = new Order("ФБ-1", client);
        List<OrderLine> lines = new ArrayList<>();
        lines.add(line(svc, "Диагностика", 500.0, 1));
        lines.add(line(null, "Разовая пайка", 1200.0, 2));
        order.setLines(lines);

        Order saved = orderRepository.saveAndFlush(order);
        em.clear();

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getLines()).hasSize(2);
        assertThat(reloaded.getTotalPrice()).isEqualTo(500.0 + 1200.0 * 2);
        assertThat(reloaded.getLines().get(0).getName()).isEqualTo("Диагностика");
        assertThat(reloaded.getLines().get(0).getServiceId()).isEqualTo(svc.getId());
        assertThat(reloaded.getLines().get(1).getServiceId()).isNull();
        assertThat(reloaded.getLines().get(1).getLineTotal()).isEqualTo(2400.0);
    }

    @Test
    void replacingLines_removesOrphans() {
        Client client = persistClient();
        Order order = new Order("ФБ-2", client);
        List<OrderLine> first = new ArrayList<>();
        first.add(line(null, "Позиция A", 100.0, 1));
        first.add(line(null, "Позиция B", 200.0, 1));
        order.setLines(first);
        Order saved = orderRepository.saveAndFlush(order);
        em.clear();

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();
        List<OrderLine> replacement = new ArrayList<>();
        replacement.add(line(null, "Позиция C", 999.0, 1));
        reloaded.setLines(replacement);
        orderRepository.saveAndFlush(reloaded);
        em.clear();

        Order after = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(after.getLines()).hasSize(1);
        assertThat(after.getLines().get(0).getName()).isEqualTo("Позиция C");
        assertThat(after.getTotalPrice()).isEqualTo(999.0);
        assertThat(orderLineRepository.count()).isEqualTo(1);
    }

    @Test
    void removeLine_dropsRowAndRecalculates() {
        Client client = persistClient();
        Order order = new Order("ФБ-3", client);
        List<OrderLine> lines = new ArrayList<>();
        lines.add(line(null, "Оставить", 300.0, 1));
        lines.add(line(null, "Убрать", 700.0, 1));
        order.setLines(lines);
        Order saved = orderRepository.saveAndFlush(order);
        em.clear();

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();
        Long toRemove = reloaded.getLines().get(1).getId();
        boolean removed = reloaded.removeLine(toRemove);
        orderRepository.saveAndFlush(reloaded);
        em.clear();

        assertThat(removed).isTrue();
        Order after = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(after.getLines()).hasSize(1);
        assertThat(after.getLines().get(0).getName()).isEqualTo("Оставить");
        assertThat(after.getTotalPrice()).isEqualTo(300.0);
    }
}
