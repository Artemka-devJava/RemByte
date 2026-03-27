package com.rembyte.model;

public enum OrderStatus {
    NEW("Новый"),
    IN_PROGRESS("В работе"),
    WAITING_FOR_PARTS("Ожидание деталей"),
    READY("Готов"),
    COMPLETED("Завершен"),
    CANCELLED("Отменен");

    public final String label;

    OrderStatus(String label) {
        this.label = label;
    }
}

