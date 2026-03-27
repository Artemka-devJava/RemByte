package com.rembyte.model;

public enum PaymentMethod {
    CASH("Наличные"),
    CARD("Карта"),
    TRANSFER("Перевод"),
    INSTALLMENT("Рассрочка");

    public final String label;

    PaymentMethod(String label) {
        this.label = label;
    }
}

