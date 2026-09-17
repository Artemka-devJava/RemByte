package com.rembyte.repository;

import com.rembyte.model.Order;
import com.rembyte.service.OrderListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // client + lines + lines.service одним запросом (JOIN — у Order всего одна
    // коллекция, cartesian product тут не грозит). Без этого сериализация
    // заказа в JSON тянула клиента, коллекцию строк и услугу каждой строки
    // отдельными SELECT-ами (N+1).

    @Override
    @EntityGraph(attributePaths = {"client", "lines", "lines.service"})
    List<Order> findAll();

    @Override
    @EntityGraph(attributePaths = {"client", "lines", "lines.service"})
    Optional<Order> findById(Long id);

    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"client", "lines", "lines.service"})
    List<Order> findByClientId(Long clientId);

    List<Order> findByStatus(String status);
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Списочный вид для /api/orders/page: проекция без строк заказа (список
     * их не показывает — см. orders.js), поэтому пагинация тут не встречается
     * с фетчем коллекции (Hibernate иначе не смог бы сделать LIMIT/OFFSET на
     * уровне SQL при join-fetch коллекции).
     */
    @Query("SELECT new com.rembyte.service.OrderListItem(" +
           "o.id, o.orderNumber, c.id, c.name, o.status, o.deviceDescription, " +
           "o.totalPrice, o.paidAmount, o.materialCost, o.createdAt) " +
           "FROM Order o JOIN o.client c " +
           "WHERE (:status = '' OR o.status = :status) " +
           "AND (:q = '' " +
           "     OR lower(o.orderNumber) LIKE lower(concat('%', :q, '%')) " +
           "     OR lower(c.name) LIKE lower(concat('%', :q, '%')) " +
           "     OR c.phone LIKE concat('%', :q, '%'))")
    Page<OrderListItem> searchOrders(@Param("status") String status, @Param("q") String q, Pageable pageable);
}
