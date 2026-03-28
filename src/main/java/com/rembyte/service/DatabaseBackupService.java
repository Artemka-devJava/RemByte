package com.rembyte.service;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис резервного копирования и восстановления базы данных FixByte CRM.
 * Использует чистый JDBC — не требует mysqldump на сервере.
 */
@Service
public class DatabaseBackupService {

    private final DataSource dataSource;

    public DatabaseBackupService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ===== РЕЗЕРВНОЕ КОПИРОВАНИЕ =====

    public void backupToStream(OutputStream outputStream) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PrintWriter w = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {

            w.println("-- FixByte CRM — Резервная копия базы данных");
            w.println("-- Создана: " + LocalDateTime.now());
            w.println("-- ==========================================");
            w.println();
            w.println("SET FOREIGN_KEY_CHECKS = 0;");
            w.println("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';");
            w.println();

            List<String> tables = getTables(conn);
            for (String table : tables) {
                w.println("-- ── Таблица: `" + table + "` ──");
                w.println("DROP TABLE IF EXISTS `" + table + "`;");

                // Схема таблицы
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                    if (rs.next()) w.println(rs.getString(2) + ";");
                }
                w.println();

                // Данные таблицы
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM `" + table + "`")) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();

                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= cols; i++) {
                        columns.add("`" + meta.getColumnName(i) + "`");
                    }
                    String colList = String.join(", ", columns);

                    while (rs.next()) {
                        List<String> values = new ArrayList<>();
                        for (int i = 1; i <= cols; i++) {
                            Object val = rs.getObject(i);
                            if (val == null) {
                                values.add("NULL");
                            } else if (val instanceof Number || val instanceof Boolean) {
                                values.add(val.toString());
                            } else {
                                values.add("'" + val.toString()
                                        .replace("\\", "\\\\")
                                        .replace("'",  "\\'")
                                        .replace("\n", "\\n")
                                        .replace("\r", "\\r") + "'");
                            }
                        }
                        w.println("INSERT INTO `" + table + "` (" + colList + ") VALUES (" +
                                String.join(", ", values) + ");");
                    }
                }
                w.println();
            }

            w.println("SET FOREIGN_KEY_CHECKS = 1;");
        }
    }

    private List<String> getTables(Connection conn) throws SQLException {
        List<String> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SHOW TABLES")) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    // ===== ВОССТАНОВЛЕНИЕ =====

    public void restoreFromStream(InputStream inputStream) throws Exception {
        String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        try (Connection conn = dataSource.getConnection()) {
            boolean prev = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                for (String stmt : splitStatements(sql)) {
                    if (!stmt.isBlank()) st.execute(stmt);
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(prev);
            }
        }
    }

    /**
     * Разбивает SQL-дамп на отдельные операторы, корректно обрабатывая
     * строковые литералы (чтобы ';' внутри INSERT не воспринималась как разделитель).
     */
    private List<String> splitStatements(String sql) {
        List<String> stmts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inString = false;
        char delim = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            // Пропуск однострочных комментариев
            if (!inString && c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') i++;
                cur.append('\n');
                continue;
            }

            if (inString) {
                cur.append(c);
                // Экранированный символ
                if (c == '\\' && i + 1 < sql.length()) {
                    cur.append(sql.charAt(++i));
                } else if (c == delim) {
                    inString = false;
                }
            } else if (c == '\'' || c == '"' || c == '`') {
                inString = true;
                delim = c;
                cur.append(c);
            } else if (c == ';') {
                String s = cur.toString().trim();
                if (!s.isEmpty()) stmts.add(s);
                cur = new StringBuilder();
            } else {
                cur.append(c);
            }
        }
        String last = cur.toString().trim();
        if (!last.isEmpty()) stmts.add(last);
        return stmts;
    }
}

