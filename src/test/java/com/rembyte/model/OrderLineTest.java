package com.rembyte.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLineTest {

    @Test
    void unitPriceNullBecomesZero() {
        OrderLine l = new OrderLine();
        l.setUnitPrice(null);
        assertThat(l.getUnitPrice()).isEqualTo(0.0);
    }

    @Test
    void quantityNullOrBelowOneBecomesOne() {
        OrderLine l = new OrderLine();
        l.setQuantity(null);
        assertThat(l.getQuantity()).isEqualTo(1);
        l.setQuantity(0);
        assertThat(l.getQuantity()).isEqualTo(1);
        l.setQuantity(-5);
        assertThat(l.getQuantity()).isEqualTo(1);
        l.setQuantity(7);
        assertThat(l.getQuantity()).isEqualTo(7);
    }

    @Test
    void lineTotalIsPriceTimesQuantity() {
        OrderLine l = new OrderLine();
        l.setUnitPrice(120.0);
        l.setQuantity(3);
        assertThat(l.getLineTotal()).isEqualTo(360.0);
    }

    @Test
    void serviceIdIsNullForOneOffLineAndIdOfLinkedService() {
        OrderLine l = new OrderLine();
        assertThat(l.getServiceId()).isNull();

        RepairService svc = new RepairService("Чистка", 500.0, "Обслуживание");
        svc.setId(9L);
        l.setService(svc);
        assertThat(l.getServiceId()).isEqualTo(9L);
    }

    @Test
    void sortOrderNullBecomesZero() {
        OrderLine l = new OrderLine();
        l.setSortOrder(null);
        assertThat(l.getSortOrder()).isEqualTo(0);
    }
}
