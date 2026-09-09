package com.rembyte.service;

import com.rembyte.model.PartItem;
import com.rembyte.model.PartLot;
import com.rembyte.model.PartsBudget;
import com.rembyte.repository.PartItemPhotoRepository;
import com.rembyte.repository.PartItemRepository;
import com.rembyte.repository.PartLotRepository;
import com.rembyte.repository.PartsBudgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartsServiceTest {

    @Mock PartItemRepository itemRepository;
    @Mock PartLotRepository lotRepository;
    @Mock PartItemPhotoRepository photoRepository;
    @Mock PartsBudgetRepository budgetRepository;

    PartsService service;

    @BeforeEach
    void setUp() {
        service = new PartsService(itemRepository, lotRepository, photoRepository, budgetRepository);
        lenient().when(itemRepository.save(any(PartItem.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(lotRepository.save(any(PartLot.class))).thenAnswer(i -> i.getArgument(0));
    }

    private PartItem item(long id, String status) {
        PartItem it = new PartItem();
        it.setId(id);
        it.setTitle("Деталь " + id);
        it.setStatus(status);
        lenient().when(itemRepository.findById(id)).thenReturn(Optional.of(it));
        return it;
    }

    // ── детали ──────────────────────────────────────────────

    @Test
    void createItem_forcesInStockStatus() {
        PartItem data = new PartItem();
        data.setTitle("RAM 8GB");
        data.setStatus("SOLD"); // попытка навязать статус игнорируется

        PartItem created = service.createItem(data);

        assertThat(created.getStatus()).isEqualTo("IN_STOCK");
        assertThat(created.getTitle()).isEqualTo("RAM 8GB");
    }

    @Test
    void sellItem_setsSoldStatusAndDefaultsDateToNow() {
        item(1L, "IN_STOCK");

        PartItem sold = service.sellItem(1L, 1500.0, null);

        assertThat(sold.getStatus()).isEqualTo("SOLD");
        assertThat(sold.getSalePrice()).isEqualTo(1500.0);
        assertThat(sold.getSaleDate()).isNotNull();
    }

    @Test
    void sellItem_rejectsNullPrice() {
        item(1L, "IN_STOCK");
        assertThatThrownBy(() -> service.sellItem(1L, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sellItem_rejectsItemThatBelongsToALot() {
        PartItem it = item(1L, "IN_STOCK");
        PartLot lot = new PartLot("Сборка ПК");
        lot.setId(3L);
        it.setLot(lot);

        assertThatThrownBy(() -> service.sellItem(1L, 1000.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("лот");
    }

    // ── лоты ────────────────────────────────────────────────

    @Test
    void createLot_requiresTitleAndAtLeastTwoItems() {
        assertThatThrownBy(() -> service.createLot("  ", List.of(1L, 2L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createLot("Лот", List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createLot("Лот", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createLot_assignsEveryItemToTheNewLot() {
        PartItem a = item(1L, "IN_STOCK");
        PartItem b = item(2L, "IN_STOCK");
        when(lotRepository.save(any(PartLot.class))).thenAnswer(i -> {
            PartLot l = i.getArgument(0);
            l.setId(10L);
            return l;
        });

        PartLot lot = service.createLot("Ноутбук на запчасти", List.of(1L, 2L));

        assertThat(a.getLot()).isSameAs(lot);
        assertThat(b.getLot()).isSameAs(lot);
    }

    @Test
    void createLot_rejectsItemAlreadyInAnotherLot() {
        PartItem a = item(1L, "IN_STOCK");
        item(2L, "IN_STOCK");
        PartLot otherLot = new PartLot("Другой");
        otherLot.setId(99L);
        a.setLot(otherLot);
        when(lotRepository.save(any(PartLot.class))).thenAnswer(i -> {
            PartLot l = i.getArgument(0);
            l.setId(10L);
            return l;
        });

        assertThatThrownBy(() -> service.createLot("Новый", List.of(1L, 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("другом лоте");
    }

    @Test
    void sellLot_marksLotAndEveryItemSoldWithSameDate() {
        PartLot lot = new PartLot("Лот");
        lot.setId(5L);
        when(lotRepository.findById(5L)).thenReturn(Optional.of(lot));
        PartItem a = new PartItem(); a.setId(1L); a.setStatus("IN_STOCK");
        PartItem b = new PartItem(); b.setId(2L); b.setStatus("IN_STOCK");
        when(itemRepository.findByLot_Id(5L)).thenReturn(List.of(a, b));

        LocalDateTime when = LocalDateTime.of(2026, 5, 1, 12, 0);
        PartLot sold = service.sellLot(5L, 9000.0, when);

        assertThat(sold.getStatus()).isEqualTo("SOLD");
        assertThat(sold.getSalePrice()).isEqualTo(9000.0);
        assertThat(a.getStatus()).isEqualTo("SOLD");
        assertThat(a.getSaleDate()).isEqualTo(when);
        assertThat(b.getSaleDate()).isEqualTo(when);
    }

    @Test
    void sellLot_rejectsEmptyLot() {
        PartLot lot = new PartLot("Пустой");
        lot.setId(5L);
        when(lotRepository.findById(5L)).thenReturn(Optional.of(lot));
        when(itemRepository.findByLot_Id(5L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.sellLot(5L, 100.0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disbandLot_detachesItemsAndDeletesLot_unlessSold() {
        PartLot lot = new PartLot("Лот");
        lot.setId(5L);
        lot.setStatus("IN_STOCK");
        when(lotRepository.findById(5L)).thenReturn(Optional.of(lot));
        PartItem a = new PartItem(); a.setId(1L);
        a.setLot(lot);
        when(itemRepository.findByLot_Id(5L)).thenReturn(List.of(a));

        service.disbandLot(5L);

        assertThat(a.getLot()).isNull();
        org.mockito.Mockito.verify(lotRepository).delete(lot);
    }

    @Test
    void disbandLot_rejectsSoldLot() {
        PartLot lot = new PartLot("Лот");
        lot.setId(5L);
        lot.setStatus("SOLD");
        when(lotRepository.findById(5L)).thenReturn(Optional.of(lot));

        assertThatThrownBy(() -> service.disbandLot(5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── бюджет ──────────────────────────────────────────────

    @Test
    void getBudget_lazilyCreatesTheSingletonRow() {
        when(budgetRepository.findById(1L)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(PartsBudget.class))).thenAnswer(i -> i.getArgument(0));

        PartsBudget b = service.getBudget();

        assertThat(b).isNotNull();
        assertThat(b.getStartingAmount()).isEqualTo(0.0);
    }

    // ── статистика ──────────────────────────────────────────

    @Test
    void statistics_balanceIsStartMinusPurchasesPlusAllRevenue() {
        PartsBudget budget = new PartsBudget();
        budget.setStartingAmount(10_000.0);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));

        PartItem inStock = new PartItem();
        inStock.setPurchasePrice(2_000.0);
        inStock.setStatus("IN_STOCK");

        PartItem soldStandalone = new PartItem();
        soldStandalone.setPurchasePrice(1_000.0);
        soldStandalone.setSalePrice(1_800.0);
        soldStandalone.setStatus("SOLD");

        PartLot soldLot = new PartLot("Лот");
        soldLot.setId(1L);
        soldLot.setStatus("SOLD");
        soldLot.setSalePrice(5_000.0);

        when(itemRepository.findAll()).thenReturn(List.of(inStock, soldStandalone));
        when(lotRepository.findAll()).thenReturn(List.of(soldLot));

        PartsStatistics stats = service.getStatistics(null, null);

        // 10000 - (2000 + 1000) + 1800 + 5000
        assertThat(stats.getCurrentBalance()).isEqualTo(13_800.0);
        assertThat(stats.getInventoryValue()).isEqualTo(2_000.0); // only the unsold item
    }
}
