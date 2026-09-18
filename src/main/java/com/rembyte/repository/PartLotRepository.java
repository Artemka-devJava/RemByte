package com.rembyte.repository;

import com.rembyte.model.PartLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartLotRepository extends JpaRepository<PartLot, Long> {

    // items — LAZY-коллекция, но PartLot.getCostTotal()/getProfit() (обычные
    // геттеры, попадают в JSON) читают её напрямую. С
    // spring.jpa.open-in-view=false транзакция к моменту сериализации уже
    // закрыта — без явного JOIN FETCH это LazyInitializationException на
    // каждый лот (GET /api/parts/lots, /api/parts/lots/{id}). DISTINCT — join
    // с *-to-many иначе даёт лот в списке по разу на каждую деталь.
    @Query("SELECT DISTINCT l FROM PartLot l LEFT JOIN FETCH l.items ORDER BY l.createdAt DESC")
    List<PartLot> findAllByOrderByCreatedAtDesc();

    @Override
    @Query("SELECT l FROM PartLot l LEFT JOIN FETCH l.items WHERE l.id = :id")
    Optional<PartLot> findById(@Param("id") Long id);
}
