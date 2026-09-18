package com.rembyte.repository;

import com.rembyte.model.PartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartItemRepository extends JpaRepository<PartItem, Long> {

    // client + lot одним запросом: без этого PartItem.getLotTitle() (обычный
    // геттер, не защищён @JsonIgnore) лезет за LAZY lot при сериализации в
    // JSON — а с spring.jpa.open-in-view=false к этому моменту транзакция
    // уже закрыта → LazyInitializationException/"no session" на каждой
    // детали, которая состоит в лоте (GET /api/parts, /api/parts/{id},
    // /api/parts?status=...).
    @Override
    @EntityGraph(attributePaths = "lot")
    Optional<PartItem> findById(Long id);

    @EntityGraph(attributePaths = "lot")
    List<PartItem> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = "lot")
    List<PartItem> findAllByOrderByCreatedAtDesc();

    List<PartItem> findByLotIsNull();
    List<PartItem> findByLot_Id(Long lotId);

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
