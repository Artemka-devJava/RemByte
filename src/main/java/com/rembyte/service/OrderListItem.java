package com.rembyte.service;

import java.time.LocalDateTime;

/**
 * Строка списка заказов (/api/orders/page). Не тянет {@code lines} — список
 * их не показывает (см. orders.js), полный заказ подгружается отдельно при
 * открытии карточки через /api/orders/{id}.
 */
public record OrderListItem(
        Long id,
        String orderNumber,
        Long clientId,
        String clientName,
        String status,
        String deviceDescription,
        Double totalPrice,
        Double paidAmount,
        LocalDateTime createdAt
) {}
