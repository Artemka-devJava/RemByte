package com.rembyte.service;

import java.time.LocalDateTime;

/** Финансовая и историческая сводка по клиенту для его карточки. */
public record ClientSummary(
        long ordersCount,
        double revenue,
        double debt,
        LocalDateTime lastOrderAt
) {}
