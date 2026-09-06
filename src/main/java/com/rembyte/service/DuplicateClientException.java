package com.rembyte.service;

import com.rembyte.model.Client;

/**
 * Клиент с таким телефоном уже есть. Несёт существующую карточку,
 * чтобы интерфейс мог предложить открыть её вместо создания дубля.
 */
public class DuplicateClientException extends RuntimeException {
    private final transient Client existing;

    public DuplicateClientException(Client existing) {
        super("Клиент с телефоном " + (existing != null ? existing.getPhone() : "") + " уже есть");
        this.existing = existing;
    }

    public Client getExisting() {
        return existing;
    }
}
