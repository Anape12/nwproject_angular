package jp.nw.security;

import static org.junit.jupiter.api.Assertions.*;

import com.mysql.cj.jdbc.MysqlDataSource;
import dev.kobun.security.Pkcs12KeyManagement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Map;
import jp.nw.entity.UserEntity;
import jp.nw.model.CodedException;
import jp.nw.model.UserInsertLogic;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BirthdayRepositoryIT {

    private static final LocalDate ORIGINAL = LocalDate.of(1996, 4, 12);
    private static Path keyFile;
    private static Map<String, String> environment;

    @BeforeAll
    static void start() throws Exception {
        if (
            !"true".equals(System.getenv("BIRTHDAY_INTEGRATION_TEST"))
        ) throw new IllegalStateException(
            "Use scripts/test-birthday.ps1 with a disposable database"
        );
        environment = System.getenv();
        keyFile = Path.of(environment.get("BIRTHDAY_KEYSTORE_PATH"));
        Files.createDirectories(keyFile.getParent());
        char[] password = BirthdayRuntime.password(environment);
        try (var ignored = Pkcs12KeyManagement.create(keyFile, password)) {
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
        BirthdayRuntime.start(environment);
    }

    @AfterAll
    static void stop() throws Exception {
        BirthdayRuntime.stop();
    }

    @Test
    @Order(1)
    void newRegistrationEncryptsBirthdayAndEnforcesFreshDatabasePermissions() throws Exception {
        create("privacyAlice", "2", ORIGINAL);
        create("privacyBob", "2", LocalDate.of(1988, 2, 3));
        create("privacyAdmin", "1", LocalDate.of(1980, 1, 1));
        create("privacySystem", "0", LocalDate.of(1978, 1, 1));
        var repo = BirthdayRuntime.repository();
        assertEquals(ORIGINAL, repo.read("privacyAlice", "privacyAlice", null, null).orElseThrow());
        assertThrows(CodedException.Denied.class, () ->
            repo.read("privacyBob", "privacyAlice", null, null)
        );
        assertEquals(ORIGINAL, repo.read("privacyAdmin", "privacyAlice", null, null).orElseThrow());
        assertThrows(CodedException.Denied.class, () ->
            repo.update("privacyAdmin", "privacySystem", ORIGINAL, null, null)
        );
        repo.update("privacyAlice", "privacyAlice", LocalDate.of(1996, 4, 13), null, null);
        assertEquals(
            LocalDate.of(1996, 4, 13),
            repo.read("privacyAlice", "privacyAlice", null, null).orElseThrow()
        );
        try (var c = admin(); var q = c.createStatement()) {
            try (
                var r = q.executeQuery(
                    "SELECT birthday,birthday_secret_id FROM users_info WHERE user_id='privacyAlice'"
                )
            ) {
                r.next();
                assertNull(r.getDate(1));
                assertNotNull(r.getString(2));
            }
            q.executeUpdate(
                "UPDATE users_info SET account_disabled=TRUE WHERE user_id='privacyAlice'"
            );
        }
        assertThrows(CodedException.Denied.class, () ->
            repo.read("privacyAlice", "privacyAlice", null, null)
        );
        try (var c = admin(); var q = c.createStatement()) {
            q.executeUpdate(
                "UPDATE users_info SET account_disabled=FALSE WHERE user_id='privacyAlice'"
            );
        }
    }

    @Test
    @Order(2)
    void swappingBusinessReferencesDoesNotExposeAnotherUsersBirthday() throws Exception {
        String original, other;
        try (
            var c = admin();
            var q = c.createStatement();
            var r = q.executeQuery(
                "SELECT user_id,birthday_secret_id FROM users_info WHERE user_id IN ('privacyAlice','privacyBob') ORDER BY user_id"
            )
        ) {
            r.next();
            original = r.getString(2);
            r.next();
            other = r.getString(2);
        }
        try (
            var c = admin();
            var p = c.prepareStatement(
                "UPDATE users_info SET birthday_secret_id=? WHERE user_id='privacyAlice'"
            )
        ) {
            // UNIQUE references reject a direct duplicate; remove the other pointer to simulate a malicious substitution.
            try (var q = c.createStatement()) {
                q.executeUpdate(
                    "UPDATE users_info SET birthday_secret_id=NULL WHERE user_id='privacyBob'"
                );
            }
            p.setString(1, other);
            p.executeUpdate();
            try {
                assertThrows(CodedException.Failure.class, () ->
                    BirthdayRuntime.repository().read("privacyAlice", "privacyAlice", null, null)
                );
            } finally {
                p.setString(1, original);
                p.executeUpdate();
                try (
                    var b = c.prepareStatement(
                        "UPDATE users_info SET birthday_secret_id=? WHERE user_id='privacyBob'"
                    )
                ) {
                    b.setString(1, other);
                    b.executeUpdate();
                }
            }
        }
    }

    @Test
    @Order(3)
    void interruptedMigrationKeepsPlaintextUntilItsReferenceIsVerifiedAndCommitted()
        throws Exception {
        // Migrate the original application seed rows first.
        while (BirthdayRuntime.repository().migrateBatch(100) > 0) {}
        try (var c = admin(); var q = c.createStatement()) {
            q.executeUpdate(
                "INSERT INTO users_info(user_id,password,birthday,permission,account_type,password_expiration,delete_flg,first_name,last_name) VALUES ('legacyPrivacyA','test','1990-03-04','2','HUMAN','20991231','0','太郎','山田'),('legacyPrivacyB','test','1991-05-06','2','HUMAN','20991231','0','花子','佐藤')"
            );
            q.execute(
                "ALTER TABLE users_info ADD CONSTRAINT stop_privacy_migration CHECK (user_id<>'legacyPrivacyB' OR birthday_secret_id IS NULL)"
            );
        }
        assertThrows(CodedException.Validation.class, () ->
            BirthdayRuntime.repository().read("legacyPrivacyA", "legacyPrivacyA", null, null)
        );
        assertThrows(java.sql.SQLException.class, () ->
            BirthdayRuntime.repository().migrateBatch(100)
        );
        try (var c = admin(); var q = c.createStatement()) {
            try (
                var r = q.executeQuery(
                    "SELECT birthday,birthday_secret_id FROM users_info WHERE user_id='legacyPrivacyA'"
                )
            ) {
                r.next();
                assertNull(r.getDate(1));
                assertNotNull(r.getString(2));
            }
            try (
                var r = q.executeQuery(
                    "SELECT birthday,birthday_secret_id FROM users_info WHERE user_id='legacyPrivacyB'"
                )
            ) {
                r.next();
                assertEquals(LocalDate.of(1991, 5, 6), r.getDate(1).toLocalDate());
                assertNull(r.getString(2));
            }
            q.execute("ALTER TABLE users_info DROP CHECK stop_privacy_migration");
        }
        assertEquals(1, BirthdayRuntime.repository().migrateBatch(100));
        assertEquals(0, BirthdayRuntime.repository().migrateBatch(100));
        assertTrue(BirthdayRuntime.repository().verifyEncryptedRecords() >= 6);
        try (
            var c = admin();
            var q = c.createStatement();
            var r = q.executeQuery(
                "SELECT detail,detail_data FROM audit_log WHERE event_category='PRIVACY'"
            )
        ) {
            while (r.next()) {
                assertNull(r.getString(1));
                assertFalse(String.valueOf(r.getString(2)).contains("1996-04-13"));
            }
        }
    }

    @Test
    @Order(4)
    void reopeningKeysRestoresAccessAndMissingKeyFailsWithoutRecreation() throws Exception {
        BirthdayRuntime.stop();
        var backup = keyFile.resolveSibling("test-backup.p12");
        Files.move(keyFile, backup);
        try {
            assertThrows(Exception.class, () -> BirthdayRuntime.start(environment));
            assertFalse(Files.exists(keyFile));
            assertThrows(CodedException.Failure.class, BirthdayRuntime::repository);
            assertThrows(CodedException.Failure.class, () -> create("privacyNoKey", "2", ORIGINAL));
        } finally {
            Files.move(backup, keyFile);
        }
        BirthdayRuntime.start(environment);
        assertEquals(
            LocalDate.of(1996, 4, 13),
            BirthdayRuntime.repository()
                .read("privacyAlice", "privacyAlice", null, null)
                .orElseThrow()
        );
        try (
            var c = admin();
            var q = c.createStatement();
            var r = q.executeQuery("SELECT COUNT(*) FROM users_info WHERE user_id='privacyNoKey'")
        ) {
            r.next();
            assertEquals(0, r.getInt(1));
        }
    }

    @Test
    @Order(5)
    void finalizedSchemaRejectsFuturePlaintextWrites() throws Exception {
        try (var c = admin(); var q = c.createStatement()) {
            q.execute(Files.readString(Path.of("scripts/sql/birthday-finalize.sql")));
            var failure = assertThrows(java.sql.SQLException.class, () ->
                q.executeUpdate(
                    "UPDATE users_info SET birthday='2001-01-01' WHERE user_id='privacyAlice'"
                )
            );
            assertEquals(3819, failure.getErrorCode());
        }
        assertEquals(
            LocalDate.of(1996, 4, 13),
            BirthdayRuntime.repository()
                .read("privacyAlice", "privacyAlice", null, null)
                .orElseThrow()
        );
    }

    private static void create(String userId, String role, LocalDate date) {
        new UserInsertLogic().insert(
            UserEntity.builder()
                .userId(userId)
                .permission(role)
                .accountType("HUMAN")
                .lastName("山田")
                .firstName("太郎")
                .birthDate(date.toString())
                .password("TemporaryTest123")
                .build(),
            LocalDate.now().plusDays(90)
        );
    }

    private static Connection admin() throws Exception {
        var source = new MysqlDataSource();
        source.setURL(environment.get("BIRTHDAY_DATA_JDBC_URL"));
        source.setUser("root");
        source.setPassword("birthday-test-root-only");
        return source.getConnection();
    }
}
