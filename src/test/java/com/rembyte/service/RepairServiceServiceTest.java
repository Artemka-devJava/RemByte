package com.rembyte.service;

import com.rembyte.model.OrderLine;
import com.rembyte.model.RepairService;
import com.rembyte.repository.OrderLineRepository;
import com.rembyte.repository.RepairServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairServiceServiceTest {

    @Mock RepairServiceRepository serviceRepository;
    @Mock OrderLineRepository orderLineRepository;

    RepairServiceService service;

    @BeforeEach
    void setUp() {
        service = new RepairServiceService(serviceRepository, orderLineRepository);
    }

    @Test
    void deleteService_detachesReferencingOrderLinesFirst() {
        OrderLine l1 = new OrderLine();
        l1.setName("снимок 1");
        RepairService svc = new RepairService("Пайка", 1000.0, "BGA");
        svc.setId(3L);
        l1.setService(svc);
        when(orderLineRepository.findByService_Id(3L)).thenReturn(List.of(l1));

        service.deleteService(3L);

        assertThat(l1.getService()).isNull();
        verify(orderLineRepository).saveAll(List.of(l1));
        verify(serviceRepository).deleteById(3L);
    }

    @Test
    void deleteService_withNoReferencesSkipsSaveAll() {
        when(orderLineRepository.findByService_Id(3L)).thenReturn(List.of());

        service.deleteService(3L);

        verify(orderLineRepository, never()).saveAll(any());
        verify(serviceRepository).deleteById(3L);
    }

    @Test
    void updateService_missingIdThrows() {
        when(serviceRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateService(9L, new RepairService("x", 1.0, "y")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void initializeDefaultServices_seedsOnlyOnEmptyCatalog() {
        when(serviceRepository.count()).thenReturn(0L);
        service.initializeDefaultServices();
        verify(serviceRepository, org.mockito.Mockito.atLeast(5)).save(any(RepairService.class));
    }

    @Test
    void initializeDefaultServices_noOpWhenCatalogHasEntries() {
        when(serviceRepository.count()).thenReturn(3L);
        service.initializeDefaultServices();
        verify(serviceRepository, never()).save(any());
    }
}
