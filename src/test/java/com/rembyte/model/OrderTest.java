package com.rembyte.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Чистая логика заказа: пересчёт суммы, работа с составом, статус оплаты,
 * хранение списков вложений строкой.
 */
class OrderTest {

    private static OrderLine line(String name, double price, int qty) {
        OrderLine l = new OrderLine();
        l.setName(name);
        l.setUnitPrice(price);
        l.setQuantity(qty);
        return l;
    }

    @Test
    void recalcTotalSumsPriceTimesQuantityAcrossLines() {
        Order order = new Order();
        order.setLines(List.of(line("A", 100.0, 2), line("B", 50.0, 3)));

        assertThat(order.getTotalPrice()).isEqualTo(350.0);
    }

    @Test
    void setLinesAssignsBackReferenceAndSequentialSortOrder() {
        Order order = new Order();
        order.setLines(List.of(line("A", 10, 1), line("B", 20, 1), line("C", 30, 1)));

        assertThat(order.getLines()).extracting(OrderLine::getOrder).containsOnly(order);
        assertThat(order.getLines()).extracting(OrderLine::getSortOrder).containsExactly(0, 1, 2);
    }

    @Test
    void setLinesNullClearsCompositionAndZeroesTotal() {
        Order order = new Order();
        order.setLines(List.of(line("A", 10, 1)));
        order.setLines(null);

        assertThat(order.getLines()).isEmpty();
        assertThat(order.getTotalPrice()).isEqualTo(0.0);
    }

    @Test
    void addLineAppendsWithNextSortOrderAndRecalculates() {
        Order order = new Order();
        order.setLines(List.of(line("A", 100, 1)));
        order.addLine(line("B", 25, 4));

        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getLines().get(1).getSortOrder()).isEqualTo(1);
        assertThat(order.getTotalPrice()).isEqualTo(200.0);
    }

    @Test
    void removeLineByIdRecalculatesAndReportsWhetherItHit() {
        Order order = new Order();
        OrderLine a = line("A", 100, 1);
        a.setId(1L);
        OrderLine b = line("B", 40, 1);
        b.setId(2L);
        order.setLines(List.of(a, b));

        assertThat(order.removeLine(2L)).isTrue();
        assertThat(order.getTotalPrice()).isEqualTo(100.0);
        assertThat(order.removeLine(999L)).isFalse();
    }

    @Test
    void paymentStatusReflectsPaidVsTotal() {
        Order order = new Order();
        order.setLines(List.of(line("A", 1000, 1)));

        order.setPaidAmount(0.0);
        assertThat(order.getPaymentStatus()).isEqualTo("Не оплачено");

        order.setPaidAmount(400.0);
        assertThat(order.getPaymentStatus()).isEqualTo("Частично оплачено");
        assertThat(order.getBalance()).isEqualTo(600.0);

        order.setPaidAmount(1000.0);
        assertThat(order.getPaymentStatus()).isEqualTo("Оплачено");
        assertThat(order.getBalance()).isEqualTo(0.0);
    }

    @Test
    void balanceNeverGoesNegativeOnOverpayment() {
        Order order = new Order();
        order.setLines(List.of(line("A", 100, 1)));
        order.setPaidAmount(250.0);

        assertThat(order.getBalance()).isEqualTo(0.0);
        assertThat(order.getPaymentStatus()).isEqualTo("Оплачено");
    }

    @Test
    void attachmentUrlsRoundTripThroughStringStorage() {
        Order order = new Order();
        order.addPhotoUrls(List.of("/a.jpg", "/b.jpg"));
        order.addPhotoUrls(List.of("/c.jpg"));
        order.addVideoUrls(List.of("/v.mp4"));

        assertThat(order.getPhotoUrls()).containsExactly("/a.jpg", "/b.jpg", "/c.jpg");
        assertThat(order.getVideoUrls()).containsExactly("/v.mp4");
        assertThat(order.getFileUrls()).isEmpty();
    }

    @Test
    void removeAttachmentUrlFindsItAcrossPhotoVideoFileLists() {
        Order order = new Order();
        order.addPhotoUrls(List.of("/p.jpg"));
        order.addVideoUrls(List.of("/v.mp4"));
        order.addFileUrls(List.of("/doc.pdf"));

        assertThat(order.removeAttachmentUrl("/v.mp4")).isTrue();
        assertThat(order.getVideoUrls()).isEmpty();
        assertThat(order.removeAttachmentUrl("/missing")).isFalse();
        assertThat(order.getPhotoUrls()).containsExactly("/p.jpg");
        assertThat(order.getFileUrls()).containsExactly("/doc.pdf");
    }
}
