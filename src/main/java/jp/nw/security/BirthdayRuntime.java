package jp.nw.security;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.kobun.security.Pkcs12KeyManagement;
import dev.kobun.security.SecurityService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import jp.nw.model.CodedException;
import jp.nw.model.ErrorCode;

/** One provider per web application. Missing configuration disables birthday operations, never stores plaintext. */
public final class BirthdayRuntime implements AutoCloseable {

    private static volatile BirthdayRuntime instance;
    private final HikariDataSource data;
    private final HikariDataSource business;
    private final HikariDataSource vault;
    private final Pkcs12KeyManagement keys;
    private final BirthdayRepository repository;

    private BirthdayRuntime(Map<String, String> env) throws Exception {
        char[] password = password(env);
        try {
            keys = Pkcs12KeyManagement.open(
                Path.of(required(env, "BIRTHDAY_KEYSTORE_PATH")),
                password
            );
        } finally {
            Arrays.fill(password, '\0');
        }
        HikariDataSource d = null,
            v = null,
            b = null;
        try {
            d = pool(
                required(env, "BIRTHDAY_DATA_JDBC_URL"),
                required(env, "DB_USER"),
                required(env, "DB_PASSWORD"),
                "birthday-data"
            );
            v = pool(
                required(env, "BIRTHDAY_VAULT_JDBC_URL"),
                required(env, "BIRTHDAY_VAULT_DB_USER"),
                required(env, "BIRTHDAY_VAULT_DB_PASSWORD"),
                "birthday-vault"
            );
            b = pool(
                required(env, "BIRTHDAY_DATA_JDBC_URL"),
                required(env, "DB_USER"),
                required(env, "DB_PASSWORD"),
                "birthday-business"
            );
            data = d;
            vault = v;
            business = b;
            repository = new BirthdayRepository(business, () ->
                SecurityService.connect(data, vault, keys)
            );
        } catch (Exception e) {
            if (b != null) b.close();
            if (d != null) d.close();
            if (v != null) v.close();
            keys.close();
            throw e;
        }
    }

    public static synchronized void start(Map<String, String> env) throws Exception {
        if (instance != null) throw new IllegalStateException(
            "Birthday runtime already initialized"
        );
        instance = new BirthdayRuntime(env);
    }

    public static BirthdayRepository repository() {
        var current = instance;
        if (current == null) throw new CodedException.Failure(ErrorCode.APP_140);
        return current.repository;
    }

    public static synchronized void stop() throws Exception {
        var current = instance;
        instance = null;
        if (current != null) current.close();
    }

    public static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(
            "Missing birthday configuration: " + name
        );
        return value;
    }

    public static char[] password(Map<String, String> env) throws Exception {
        return Files.readString(
            Path.of(required(env, "BIRTHDAY_KEYSTORE_PASSWORD_FILE")),
            StandardCharsets.UTF_8
        )
            .stripTrailing()
            .toCharArray();
    }

    private static HikariDataSource pool(String url, String user, String password, String name) {
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setPoolName(name);
        config.setMaximumPoolSize(6);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5000);
        config.setInitializationFailTimeout(5000);
        return new HikariDataSource(config);
    }

    @Override
    public void close() throws Exception {
        try {
            business.close();
            data.close();
            vault.close();
        } finally {
            keys.close();
        }
    }
}
