package com.rembyte.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/** Проверяет определение бинарных колонок (лёгкий бэкап пропускает такие таблицы). */
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
        }
    }
}
