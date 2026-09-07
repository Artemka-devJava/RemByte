package com.rembyte.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Сервис резервного копирования и восстановления базы данных FixByte CRM.
 * Использует чистый JDBC — не требует mysqldump на сервере.
 */
@Service
public class DatabaseBackupService {

    private static final int MAX_RESTORE_ATTEMPTS = 2;

    private final DataSource dataSource;

    @Value("${fixbyte.upload.dir:uploads}")
    private String uploadDir;

    public DatabaseBackupService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ===== РЕЗЕРВНОЕ КОПИРОВАНИЕ =====

    public void backupToStream(OutputStream outputStream) throws Exception {
        backupToStream(outputStream, true);
    }

    public void backupToStream(OutputStream outputStream, boolean includeLegacyUploads) throws Exception {
        byte[] sqlBytes = buildDatabaseDump();

        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(outputStream), StandardCharsets.UTF_8)) {
            ZipEntry sqlEntry = new ZipEntry("database.sql");
            zip.putNextEntry(sqlEntry);
            zip.write(sqlBytes);
            zip.closeEntry();

            ZipEntry meta = new ZipEntry("backup-info.txt");
            zip.putNextEntry(meta);
            String metaText = "createdAt=" + LocalDateTime.now() + "\n"
                    + "format=fixbyte-backup-v2\n"
                    + "includesLegacyUploads=" + includeLegacyUploads + "\n";
            zip.write(metaText.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            if (includeLegacyUploads) {
                addUploadsToZip(zip);
            }
        }
    }

    private byte[] buildDatabaseDump() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            if (isH2(conn)) {
                // H2 (локальный fallback без MariaDB) не понимает "SHOW CREATE TABLE" —
                // используем встроенный SCRIPT, который сам корректно упорядочивает
                // CREATE TABLE/ALTER TABLE ADD CONSTRAINT и данные.
                return buildH2ScriptDump(conn);
            }
            return buildMariaDbDump(conn);
        }
    }

    private boolean isH2(Connection conn) throws SQLException {
        String product = conn.getMetaData().getDatabaseProductName();
        return product != null && product.toUpperCase(Locale.ROOT).contains("H2");
    }

    private byte[] buildH2ScriptDump(Connection conn) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(32 * 1024);
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SCRIPT NOPASSWORDS NOSETTINGS DROP")) {

            w.println("-- FixByte CRM — Резервная копия базы данных (H2)");
            w.println("-- Создана: " + LocalDateTime.now());
            w.println("-- ==========================================");
            w.println();
            while (rs.next()) {
                w.println(rs.getString(1));
            }
            w.flush();
            return baos.toByteArray();
        }
    }

    private byte[] buildMariaDbDump(Connection conn) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(32 * 1024);
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

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

                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                    if (rs.next()) {
                        w.println(rs.getString(2) + ";");
                    }
                }
                w.println();

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
                        List<String> values = new ArrayList<>(cols);
                        for (int i = 1; i <= cols; i++) {
                            Object val = rs.getObject(i);
                            values.add(toSqlValue(val));
                        }
                        w.println("INSERT INTO `" + table + "` (" + colList + ") VALUES (" + String.join(", ", values) + ");");
                    }
                }
                w.println();
            }

            w.println("SET FOREIGN_KEY_CHECKS = 1;");
            w.flush();
            return baos.toByteArray();
        }
    }

    private String toSqlValue(Object val) throws SQLException {
        if (val == null) {
            return "NULL";
        }
        if (val instanceof Boolean b) {
            return b ? "1" : "0";
        }
        if (val instanceof Number) {
            return val.toString();
        }
        if (val instanceof byte[] bytes) {
            return "0x" + toHex(bytes);
        }
        if (val instanceof Blob blob) {
            long len = blob.length();
            if (len <= 0) {
                return "0x";
            }
            if (len > Integer.MAX_VALUE) {
                throw new SQLException("Слишком большой BLOB для дампа: " + len);
            }
            return "0x" + toHex(blob.getBytes(1, (int) len));
        }
        return "'" + escapeSql(val.toString()) + "'";
    }

    private String escapeSql(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }

    private void addUploadsToZip(ZipOutputStream zip) throws IOException {
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadRoot) || !Files.isDirectory(uploadRoot)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(uploadRoot)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String relative = uploadRoot.relativize(path).toString().replace('\\', '/');
                String entryName = "uploads/" + relative;
                try {
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zip);
                    zip.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
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
        try (PushbackInputStream pb = new PushbackInputStream(new BufferedInputStream(inputStream), 4)) {
            byte[] signature = pb.readNBytes(4);
            if (signature.length > 0) {
                pb.unread(signature);
            }

            if (isZipSignature(signature)) {
                restoreFromZip(pb);
            } else {
                restoreSql(pb);
            }
        }
    }

    private boolean isZipSignature(byte[] signature) {
        return signature.length >= 4
                && signature[0] == 'P'
                && signature[1] == 'K'
                && signature[2] == 3
                && signature[3] == 4;
    }

    private void restoreFromZip(InputStream inputStream) throws Exception {
        byte[] sqlBytes = null;
        boolean uploadDirCleaned = false;
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();

        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(inputStream), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zip.closeEntry();
                    continue;
                }

                String entryName = entry.getName();
                if ("database.sql".equals(entryName)) {
                    sqlBytes = zip.readAllBytes();
                    zip.closeEntry();
                    continue;
                }

                if (entryName.startsWith("uploads/")) {
                    if (!uploadDirCleaned) {
                        cleanDirectory(uploadRoot);
                        uploadDirCleaned = true;
                    }

                    String relativeName = entryName.substring("uploads/".length());
                    Path target = uploadRoot.resolve(relativeName).normalize();
                    if (!target.startsWith(uploadRoot)) {
                        throw new IOException("Недопустимый путь в архиве: " + entryName);
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }

                zip.closeEntry();
            }
        }

        if (sqlBytes == null || sqlBytes.length == 0) {
            throw new IllegalArgumentException("В архиве не найден файл database.sql");
        }
        restoreSql(new ByteArrayInputStream(sqlBytes));
    }

    private void cleanDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            return;
        }

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .filter(path -> !path.equals(dir))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    private void restoreSql(InputStream inputStream) throws Exception {
        String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        List<String> statements = splitStatements(sql);

        for (int attempt = 1; attempt <= MAX_RESTORE_ATTEMPTS; attempt++) {
            try {
                executeRestoreStatements(statements);
                return;
            } catch (SQLException ex) {
                if (!isRecoverableRestoreError(ex) || attempt >= MAX_RESTORE_ATTEMPTS) {
                    throw ex;
                }
            }
        }
    }

    private void executeRestoreStatements(List<String> statements) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            boolean prev = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                int statementIndex = 0;
                for (String stmt : statements) {
                    String trimmed = stmt == null ? "" : stmt.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    statementIndex++;
                    try {
                        st.execute(trimmed);
                    } catch (SQLException sqlEx) {
                        throw new SQLException(
                                "Ошибка выполнения SQL-оператора #" + statementIndex + ": " + previewSql(trimmed),
                                sqlEx
                        );
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(prev);
            }
        }
    }

    private boolean isRecoverableRestoreError(SQLException ex) {
        if (ex == null) {
            return false;
        }

        if (ex instanceof SQLNonTransientConnectionException
                || ex instanceof SQLTransientConnectionException
                || ex instanceof SQLRecoverableException) {
            return true;
        }

        String sqlState = ex.getSQLState();
        if (sqlState != null && sqlState.startsWith("08")) {
            return true;
        }

        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof SocketException) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    private String previewSql(String sql) {
        String normalized = sql.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160) + "...";
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

