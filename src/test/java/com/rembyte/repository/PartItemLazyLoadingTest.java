package com.rembyte.repository;

import com.rembyte.model.PartItem;
import com.rembyte.model.PartLot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регресс на баг из spring.jpa.open-in-view=false (коммит 6373af3): без
 * {@code @EntityGraph} на {@code lot}/{@code items} обычные Mockito-тесты
 * сервисов не ловят LazyInitializationException — моки не воспроизводят
 * реальное закрытие Hibernate-сессии. Здесь {@code em.clear()} после
 * запроса имитирует ровно это (OSIV выключен → сессия закрывается до
 * сериализации ответа контроллером): у detached-сущности с ещё не
 * инициализированным lazy-proxy обращение к полю бросает то же исключение,
 * что ловили на GET /api/parts и /api/parts/lots в реальном приложении.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PartItemLazyLoadingTest {

    @Autowired TestEntityManager em;
    @Autowired PartItemRepository itemRepository;
    @Autowired PartLotRepository lotRepository;

    @Test
    void findAllByOrderByCreatedAtDesc_lotSurvivesDetachSoGetLotTitleWorks() {
        PartLot lot = new PartLot("Test Lot");
        em.persist(lot);
        PartItem item = new PartItem();
        item.setTitle("Test Part");
        item.setLot(lot);
        em.persist(item);
        em.flush();
        em.clear(); // отрывает уже загруженные сущности от персистентного контекста

        List<PartItem> items = itemRepository.findAllByOrderByCreatedAtDesc();
        em.clear(); // и то, что вернул сам запрос — тоже, до чтения geLotTitle()

        assertThat(items).extracting(PartItem::getLotTitle).containsExactly("Test Lot");
    }

    @Test
    void findById_lotSurvivesDetachSoGetLotTitleWorks() {
        PartLot lot = new PartLot("Test Lot");
        em.persist(lot);
        PartItem item = new PartItem();
        item.setTitle("Test Part");
        item.setLot(lot);
        em.persist(item);
        em.flush();
        Long id = item.getId();
        em.clear();

        Optional<PartItem> found = itemRepository.findById(id);
        em.clear();

        assertThat(found).isPresent();
        assertThat(found.get().getLotTitle()).isEqualTo("Test Lot");
    }

    @Test
    void lotFindAllByOrderByCreatedAtDesc_itemsSurviveDetachAndAreNotDuplicated() {
        PartLot lot = new PartLot("Test Lot");
        em.persist(lot);
        PartItem a = new PartItem();
        a.setTitle("A");
        a.setPurchasePrice(100.0);
        a.setLot(lot);
        em.persist(a);
        PartItem b = new PartItem();
        b.setTitle("B");
        b.setPurchasePrice(50.0);
        b.setLot(lot);
        em.persist(b);
        em.flush();
        em.clear();

        List<PartLot> lots = lotRepository.findAllByOrderByCreatedAtDesc();
        em.clear();

        // ровно один лот в списке (не по разу на каждую входящую деталь — see DISTINCT в @Query)
        assertThat(lots).hasSize(1);
        assertThat(lots.get(0).getItems()).hasSize(2);
        assertThat(lots.get(0).getCostTotal()).isEqualTo(150.0);
    }

    @Test
    void lotFindById_itemsSurviveDetach() {
        PartLot lot = new PartLot("Test Lot");
        em.persist(lot);
        PartItem item = new PartItem();
        item.setTitle("A");
        item.setPurchasePrice(100.0);
        item.setLot(lot);
        em.persist(item);
        em.flush();
        Long lotId = lot.getId();
        em.clear();

        Optional<PartLot> found = lotRepository.findById(lotId);
        em.clear();

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getCostTotal()).isEqualTo(100.0);
    }
}
