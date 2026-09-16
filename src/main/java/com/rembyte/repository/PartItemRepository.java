package com.rembyte.repository;

import com.rembyte.model.PartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PartItemRepository extends JpaRepository<PartItem, Long> {
    List<PartItem> findByStatusOrderByCreatedAtDesc(String status);
    List<PartItem> findAllByOrderByCreatedAtDesc();
    List<PartItem> findByLotIsNull();
    List<PartItem> findByLot_Id(Long lotId);
    List<PartItem> findByPurchaseDateBetween(LocalDateTime from, LocalDateTime to);
    List<PartItem> findByStatusAndSaleDateBetween(String status, LocalDateTime from, LocalDateTime to);

    /**
     * Постраничный список проданных деталей (parts.js: категория «🔴 Продано»)
     * — единственное место на странице «Комплектующие», где данные копятся
     * без ограничения (склад в наличии сгруппирован по категориям, полная
     * пагинация сломала бы эту группировку). @EntityGraph — lot тянется LAZY
     * (PartItem.getLotTitle() иначе N+1 на каждую деталь в лоте).
     */
    @EntityGraph(attributePaths = "lot")
    Page<PartItem> findByStatus(String status, Pageable pageable);
}
