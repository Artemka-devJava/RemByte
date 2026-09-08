package com.rembyte.service;

/**
 * Уровень резервной копии.
 *
 * <ul>
 *   <li>{@link #LIGHT} — только записи БД без бинарных данных: таблицы фото и
 *       вложений выгружаются структурой без строк, папка uploads не включается.
 *       Файл маленький, подходит для частого/быстрого бэкапа настроек и данных.</li>
 *   <li>{@link #FULL} — всё целиком: полный дамп БД (включая BLOB-вложения),
 *       плюс legacy-папка uploads. Из такого бэкапа восстанавливается вообще всё.</li>
 * </ul>
 */
public enum BackupLevel {
    LIGHT,
    FULL;

    public static BackupLevel parse(String value) {
        if (value == null) {
            return FULL;
        }
        return switch (value.trim().toLowerCase()) {
            case "light", "db", "lite", "records" -> LIGHT;
            default -> FULL;
        };
    }

    public String fileSuffix() {
        return this == LIGHT ? "_light" : "_full";
    }
}
