package com.rembyte.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Бэкап: определение бинарных колонок + сохранение/восстановление фото (BLOB). */
class DatabaseBackupServiceTest {

    private SimpleDriverDataSource ds;
    private DatabaseBackupService service;

    @BeforeEach
    void setUp() throws Exception {
        ds = new SimpleDriverDataSource();
        ds.setDriverClass(org.h2.Driver.class);
        ds.setUrl("jdbc:h2:mem:backupcols;DB_CLOSE_DELAY=-1;MODE=MariaDB");
        ds.setUsername("sa");
        ds.setPassword("");
        service = new DatabaseBackupService(ds);

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE clients (id BIGINT PRIMARY KEY, name VARCHAR(200), phone VARCHAR(40))");
            st.execute("CREATE TABLE order_attachments (id BIGINT PRIMARY KEY, name VARCHAR(200), content BLOB NOT NULL)");
            st.execute("CREATE TABLE client_photos (id BIGINT PRIMARY KEY, content LONGBLOB NOT NULL)");
            st.execute("CREATE TABLE part_item_photos (id BIGINT PRIMARY KEY, original_name VARCHAR(255), "
                    + "content_type VARCHAR(150), content LONGBLOB NOT NULL)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        }
    }

    @Test
    void detectsBinaryColumns() throws Exception {
        try (Connection c = ds.getConnection()) {
            assertThat(service.hasBinaryColumn(c, "clients")).isFalse();
            assertThat(service.hasBinaryColumn(c, "order_attachments")).isTrue();
            assertThat(service.hasBinaryColumn(c, "client_photos")).isTrue();
            assertThat(service.hasBinaryColumn(c, "part_item_photos")).isTrue();
        }
    }

    /** Сериализация строк: байты фото попадают в дамп как 0x-хекс и восстанавливаются 1-в-1. */
    @Test
    void writeRowInsertsKeepsPhotoBytes() throws Exception {
        byte[] photo = new byte[4096];
        for (int i = 0; i < photo.length; i++) {
            photo[i] = (byte) ((i * 37 + 11) & 0xFF);
        }
        insertPhoto(1, "board.jpg", "image/jpeg", photo);

        StringWriter sw = new StringWriter();
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `part_item_photos`");
             PrintWriter w = new PrintWriter(sw)) {
            service.writeRowInserts(rs, "part_item_photos", w);
        }
        String insert = sw.toString();
        assertThat(insert).contains("0x" + HexFormat.of().formatHex(photo));

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM part_item_photos");
            st.execute(insert.trim().replaceAll(";\\s*$", ""));
        }
        assertThat(readPhoto(1)).isEqualTo(photo);
    }

    /** Пустой BLOB сериализуется как '' (а не как невалидный 0x). */
    @Test
    void writeRowInsertsHandlesEmptyBlob() throws Exception {
        insertPhoto(2, "empty.bin", "application/octet-stream", new byte[0]);
        StringWriter sw = new StringWriter();
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `part_item_photos`");
             PrintWriter w = new PrintWriter(sw)) {
            service.writeRowInserts(rs, "part_item_photos", w);
        }
        assertThat(sw.toString()).contains("''").doesNotContain("0x,");
    }

    /** Бэкап от MariaDB-деплоя, принесённый на H2-инстанс (разные диалекты SQL) — должен
        упасть сразу с понятным объяснением, а не с нечитаемой синтаксической ошибкой
        посреди разбора незнакомых H2-парсеру конструкций. */
    @Test
    void restoreRejectsMariaDbDumpOnH2WithClearMessage() {
        String mariaDbStyleSql = "SET FOREIGN_KEY_CHECKS = 0;\nSELECT 1;\nSET FOREIGN_KEY_CHECKS = 1;\n";
        byte[] bytes = mariaDbStyleSql.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.restoreFromStream(new ByteArrayInputStream(bytes)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("MariaDB")
                .hasMessageContaining("H2");
    }

    /** Полный бэкап (ZIP, H2 SCRIPT-ветка) сохраняет и восстанавливает фото детали. */
    @Test
    void fullZipRoundTripKeepsPhoto() throws Exception {
        byte[] photo = new byte[2048];
        for (int i = 0; i < photo.length; i++) {
            photo[i] = (byte) (255 - (i & 0xFF));
        }
        insertPhoto(3, "p.jpg", "image/jpeg", photo);

        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        service.backupToStream(zip, BackupLevel.FULL);

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM part_item_photos");
        }
        service.restoreFromStream(new ByteArrayInputStream(zip.toByteArray()));

        assertThat(readPhoto(3)).isEqualTo(photo);
    }

    /** Бэкап "из старой версии" с UTF-8 BOM в начале database.sql — раньше ронял
        самый первый оператор синтаксической ошибкой (BOM не входит в String.trim()),
        хотя в превью текста ошибки символ не виден. */
    @Test
    void restoreStripsLeadingUtf8Bom() throws Exception {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("INSERT INTO clients (id, name, phone) VALUES (1, 'Old', '000')");
        }
        // Без "SET FOREIGN_KEY_CHECKS"/H2-шапки намеренно — иначе сработает проверка
        // диалекта (requireMatchingDialect) раньше, чем тест успеет дойти до BOM.
        String sql = "DELETE FROM clients;\n"
                + "INSERT INTO clients (id, name, phone) VALUES (1, 'Restored', '111');\n";
        byte[] bomPrefixed = ("﻿" + sql).getBytes(StandardCharsets.UTF_8);

        service.restoreFromStream(new ByteArrayInputStream(bomPrefixed));

        try (Connection c = ds.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM clients WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("Restored");
        }
    }

    /** Лёгкий бэкап (ZIP) не содержит байтов фото, но таблица в дампе присутствует. */
    @Test
    void lightZipBackupOmitsPhotoBytes() throws Exception {
        byte[] photo = "PHOTO-BYTES-ABCDEF-0123456789".getBytes(StandardCharsets.UTF_8);
        insertPhoto(4, "x.png", "image/png", photo);

        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        service.backupToStream(zip, BackupLevel.LIGHT);
        String sql = databaseSqlFromZip(zip.toByteArray());

        assertThat(sql).doesNotContain(HexFormat.of().formatHex(photo).toUpperCase());
        assertThat(sql.toUpperCase()).contains("PART_ITEM_PHOTOS"); // структура таблицы есть
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void insertPhoto(long id, String name, String type, byte[] content) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO part_item_photos (id, original_name, content_type, content) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, id);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setBytes(4, content);
            ps.executeUpdate();
        }
    }

    private byte[] readPhoto(long id) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT content FROM part_item_photos WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getBytes(1);
            }
        }
    }

    private static String databaseSqlFromZip(byte[] zipBytes) throws Exception {
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if ("database.sql".equals(e.getName())) {
                    return new String(zin.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("database.sql не найден в архиве");
    }
}
