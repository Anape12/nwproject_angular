package jp.nw.model;

import static org.junit.jupiter.api.Assertions.*;

import com.mysql.cj.jdbc.MysqlDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditLogSearchIT {

    @Test
    void migrationPreservesLogsAndRepairsSearchAcrossDifferentCollations() throws Exception {
        if (!"true".equals(System.getenv("BIRTHDAY_INTEGRATION_TEST"))) {
            throw new IllegalStateException("Use scripts/test-birthday.ps1 with a disposable database");
        }
        var source = new MysqlDataSource();
        source.setURL(System.getenv("BIRTHDAY_DATA_JDBC_URL"));
        source.setUser("root");
        source.setPassword("birthday-test-root-only");
        String actor = "auditRegression";
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            // Reproduce a V20 database that inherited the MySQL 8 default collation.
            statement.execute("ALTER TABLE audit_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            AuditLogLogic.recordStructured(connection, actor, "AUTH", "LOGIN_FAILED", "USER", actor,
                    false, "127.0.0.1", "integration-test", "BAD_PASSWORD", Map.of("failedCount", 2));
            AuditLogLogic.record(connection, actor, "AUTH", "LOGIN_LEGACY", "USER", actor,
                    true, "127.0.0.1", "integration-test", "山田太郎の過去の監査記録");
            AuditLogLogic.recordStructured(connection, actor, "PRIVACY", "UNKNOWN_ACTION", "USER", actor,
                    false, "127.0.0.1", "integration-test", "UNKNOWN_REASON", Map.of());

            var failure = assertThrows(CodedException.Failure.class,
                    () -> AuditLogLogic.search(actor, null, null, null, null, null, 500));
            assertEquals(1267, ((SQLException) failure.getCause()).getErrorCode());
            long before;
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM audit_log")) {
                rows.next();
                before = rows.getLong(1);
            }
            statement.execute(Files.readString(Path.of(".devcontainer/migration/V27__align_audit_log_collation.sql")));
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM audit_log")) {
                rows.next();
                assertEquals(before, rows.getLong(1));
            }
        }

        var all = AuditLogLogic.search(actor, null, null, null, null, null, 500);
        assertEquals(3, all.size());
        assertEquals("山田太郎の過去の監査記録", all.stream()
                .filter(row -> "LOGIN_LEGACY".equals(row.getEventAction())).findFirst().orElseThrow().getDisplayDetail());
        assertEquals("UNKNOWN_REASON", all.stream()
                .filter(row -> "UNKNOWN_ACTION".equals(row.getEventAction())).findFirst().orElseThrow().getDisplayDetail());
        assertEquals(2, AuditLogLogic.search(actor, "AUTH", null, null, null, null, 500).size());
        var filtered = AuditLogLogic.search(actor, "AUTH", "FAILED", false,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 10);
        assertEquals(1, filtered.size());
        assertEquals("パスワードが一致しません（失敗回数: 2）。", filtered.getFirst().getDisplayDetail());
        assertTrue(AuditLogLogic.search(actor, "AUTH", null, null,
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(9), 500).isEmpty());
    }
}
