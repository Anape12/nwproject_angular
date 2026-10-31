package jp.nw.security;

import dev.kobun.security.Pkcs12KeyManagement;
import java.nio.file.Path;
import java.util.Arrays;

/** Offline management only: never exposed as a servlet or called automatically at application startup. */
public final class BirthdayAdmin {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException(
            "Expected init-keys, migrate or verify"
        );
        var env = System.getenv();
        if (args[0].equals("init-keys")) {
            char[] password = BirthdayRuntime.password(env);
            try (
                var keys = Pkcs12KeyManagement.create(
                    Path.of(BirthdayRuntime.required(env, "BIRTHDAY_KEYSTORE_PATH")),
                    password
                )
            ) {
                System.out.println("Birthday keystore initialized.");
            } finally {
                Arrays.fill(password, '\0');
            }
            return;
        }
        if (
            !args[0].equals("migrate") && !args[0].equals("verify")
        ) throw new IllegalArgumentException("Unknown operation");
        BirthdayRuntime.start(env);
        try {
            if (args[0].equals("migrate")) {
                int total = 0,
                    count;
                while ((count = BirthdayRuntime.repository().migrateBatch(100)) > 0) {
                    total += count;
                    System.out.println("Committed migration records: " + total);
                }
            }
            System.out.println(
                "Verified encrypted records: " +
                    BirthdayRuntime.repository().verifyEncryptedRecords()
            );
        } finally {
            BirthdayRuntime.stop();
        }
    }
}
