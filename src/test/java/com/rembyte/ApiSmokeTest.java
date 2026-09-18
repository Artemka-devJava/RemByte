package com.rembyte;

import com.rembyte.model.Client;
import com.rembyte.model.Order;
import com.rembyte.model.PartItem;
import com.rembyte.model.PartLot;
import com.rembyte.repository.ClientRepository;
import com.rembyte.repository.OrderRepository;
import com.rembyte.repository.PartItemRepository;
import com.rembyte.repository.PartLotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Смок-тест поверх реального Spring-контекста (не моки): бьёт по ключевым
 * GET-эндпоинтам после создания настоящих данных через репозитории.
 * <p>
 * Цель — ловить то, что обычные Mockito-тесты сервисов принципиально не
 * видят: ошибки при СЕРИАЛИЗАЦИИ ответа в JSON, когда транзакция уже
 * закрыта ({@code spring.jpa.open-in-view=false} — см. class-level javadoc
 * в {@code GlobalExceptionHandler} и историю в памяти проекта: без entity
 * graph на LAZY-связь, которую читает незащищённый {@code @JsonIgnore}
 * геттер, это 400/500 "LazyInitializationException" на живом сервере,
 * а 184 существующих юнит-теста этого не замечают, потому что мок не
 * воспроизводит реальное закрытие Hibernate-сессии).
 * <p>
 * Намеренно НЕ {@code @Transactional} — тестовая транзакция держала бы
 * сессию открытой на весь метод теста, включая вызовы MockMvc, и маскировала
 * бы ровно тот баг, для которого этот тест написан. Данные остаются в
 * H2 in-memory базе до конца контекста — не страшно, БД разовая для теста.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class ApiSmokeTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClientRepository clientRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PartItemRepository partItemRepository;
    @Autowired PartLotRepository partLotRepository;

    @Test
    void ordersClientsAndPartsListingsSerializeWithoutError() throws Exception {
        Client client = new Client("Smoke Client", "+79995550011");
        client = clientRepository.save(client);

        Order order = new Order();
        order.setOrderNumber("SMOKE-1");
        order.setClient(client);
        order = orderRepository.save(order);

        PartLot lot = new PartLot("Smoke Lot");
        lot = partLotRepository.save(lot);
        PartItem item = new PartItem();
        item.setTitle("Smoke Part");
        item.setLot(lot);
        partItemRepository.save(item);

        mockMvc.perform(get("/api/clients")).andExpect(status().isOk());
        mockMvc.perform(get("/api/clients/page")).andExpect(status().isOk());
        mockMvc.perform(get("/api/clients/{id}", client.getId())).andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")).andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/page")).andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/{id}", order.getId())).andExpect(status().isOk());

        // Основной регресс, ради которого написан весь этот тест: деталь
        // в лоте должна сериализоваться и в списках, и по отдельности.
        mockMvc.perform(get("/api/parts")).andExpect(status().isOk());
        mockMvc.perform(get("/api/parts/{id}", item.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/api/parts?status=IN_STOCK")).andExpect(status().isOk());
        mockMvc.perform(get("/api/parts/lots")).andExpect(status().isOk());
        mockMvc.perform(get("/api/parts/lots/{id}", lot.getId())).andExpect(status().isOk());
    }
}
