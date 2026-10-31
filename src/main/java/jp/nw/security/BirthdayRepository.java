package jp.nw.security;

import dev.kobun.security.SecurityService;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import jp.nw.model.AuditLogLogic;
import jp.nw.model.CodedException;
import jp.nw.model.ErrorCode;

/** Owns the business transaction; encrypted records are published before their reference is committed. */
public final class BirthdayRepository {

    @FunctionalInterface
    public interface Sessions {
        SecurityService open() throws SQLException;
    }

    private final DataSource business;
    private final Sessions sessions;

    public BirthdayRepository(DataSource business, Sessions sessions) {
        this.business = business;
        this.sessions = sessions;
    }

    public Optional<LocalDate> read(String actor, String target, String ip, String agent)
        throws SQLException {
        try (var c = business.getConnection()) {
            c.setAutoCommit(false);
            try {
                var accounts = authorize(c, actor, target);
                Optional<LocalDate> date;
                try (
                    var p = c.prepareStatement(
                        "SELECT birthday,birthday_secret_id FROM users_info WHERE id=?"
                    )
                ) {
                    p.setInt(1, accounts.id());
                    try (var r = p.executeQuery()) {
                        r.next();
                        String ref = r.getString(2);
                        if (
                            ref == null && r.getDate(1) != null
                        ) throw new CodedException.Validation(ErrorCode.APP_142);
                        date =
                            ref == null
                                ? Optional.empty()
                                : Optional.of(readSecret(accounts.id(), ref));
                    }
                }
                audit(c, actor, target, "BIRTHDAY_VIEWED", ip, agent);
                c.commit();
                return date;
            } catch (SQLException | RuntimeException e) {
                rollback(c, e);
                throw e;
            }
        }
    }

    public void update(String actor, String target, LocalDate date, String ip, String agent)
        throws SQLException {
        BirthdayPolicy.validate(date);
        try (var c = business.getConnection()) {
            c.setAutoCommit(false);
            try {
                var account = authorize(c, actor, target);
                attach(c, account.id(), date);
                audit(c, actor, target, "BIRTHDAY_CHANGED", ip, agent);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(c, e);
                throw e;
            }
        }
    }

    /** Caller owns a transaction and the user row (new registration or FOR UPDATE). */
    public void attach(Connection c, int ownerId, LocalDate date) throws SQLException {
        if (c.getAutoCommit()) throw new SQLException(
            "Birthday attachment requires a business transaction"
        );
        BirthdayPolicy.validate(date);
        String ref;
        try (var secrets = sessions.open()) {
            ref = secrets.create(binding(ownerId) + "\n" + date);
            if (!date.equals(decode(ownerId, secrets.read(ref)))) throw new SecurityException(
                "Verification failed"
            );
        } catch (SQLException | RuntimeException e) {
            throw new CodedException.Failure(ErrorCode.APP_140, e);
        }
        try (
            var p = c.prepareStatement(
                "UPDATE users_info SET birthday=NULL,birthday_secret_id=? WHERE id=?"
            )
        ) {
            p.setString(1, ref);
            p.setInt(2, ownerId);
            if (p.executeUpdate() != 1) throw new SQLException(
                "User disappeared during birthday attachment"
            );
        }
        // Do not delete the previous/new secret on failure: a lost commit acknowledgement
        // may mean this reference is already durable. Orphan collection is a later operation.
    }

    /** Commits each row separately. Re-running selects only remaining legacy rows. */
    public int migrateBatch(int limit) throws SQLException {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException(
            "Batch size must be 1..1000"
        );
        var ids = new ArrayList<Integer>();
        try (
            var c = business.getConnection();
            var p = c.prepareStatement(
                "SELECT id FROM users_info WHERE birthday IS NOT NULL AND birthday_secret_id IS NULL ORDER BY id LIMIT ?"
            )
        ) {
            p.setInt(1, limit);
            try (var r = p.executeQuery()) {
                while (r.next()) ids.add(r.getInt(1));
            }
        }
        int migrated = 0;
        for (int id : ids) {
            try (var c = business.getConnection()) {
                c.setAutoCommit(false);
                try (
                    var p = c.prepareStatement(
                        "SELECT user_id,birthday,birthday_secret_id FROM users_info WHERE id=? FOR UPDATE"
                    )
                ) {
                    p.setInt(1, id);
                    try (var r = p.executeQuery()) {
                        if (r.next() && r.getDate(2) != null && r.getString(3) == null) {
                            String target = r.getString(1);
                            attach(c, id, r.getDate(2).toLocalDate());
                            audit(c, null, target, "BIRTHDAY_MIGRATED", null, null);
                            migrated++;
                        }
                    }
                    c.commit();
                } catch (SQLException | RuntimeException e) {
                    rollback(c, e);
                    throw e;
                }
            }
        }
        return migrated;
    }

    public int verifyEncryptedRecords() throws SQLException {
        int count = 0;
        try (
            var c = business.getConnection();
            var p = c.prepareStatement("SELECT id,birthday,birthday_secret_id FROM users_info");
            var r = p.executeQuery()
        ) {
            while (r.next()) {
                if (r.getDate(2) != null) throw new CodedException.Validation(ErrorCode.APP_142);
                if (r.getString(3) != null) {
                    readSecret(r.getInt(1), r.getString(3));
                    count++;
                }
            }
        }
        return count;
    }

    private LocalDate readSecret(int ownerId, String ref) {
        try (var secrets = sessions.open()) {
            return decode(ownerId, secrets.read(ref));
        } catch (SQLException | RuntimeException e) {
            throw new CodedException.Failure(ErrorCode.APP_140, e);
        }
    }

    private static String binding(int id) {
        if (id <= 0) throw new IllegalArgumentException("Owner ID must be positive");
        return "nwproject:users_info:" + id + ":birthday:v1";
    }

    static LocalDate decode(int id, String authenticatedPlaintext) {
        String prefix = binding(id) + "\n";
        if (!authenticatedPlaintext.startsWith(prefix)) throw new SecurityException(
            "Birthday ownership mismatch"
        );
        return LocalDate.parse(authenticatedPlaintext.substring(prefix.length()));
    }

    private BirthdayPolicy.Account authorize(Connection c, String actor, String target)
        throws SQLException {
        BirthdayPolicy.Account a = null,
            t = null;
        // Consistent lock order serializes role changes and concurrent birthday writes.
        try (
            var p = c.prepareStatement(
                "SELECT id,user_id,permission,account_type,account_disabled,delete_flg FROM users_info WHERE user_id IN (?,?) ORDER BY id FOR UPDATE"
            )
        ) {
            p.setString(1, actor);
            p.setString(2, target);
            try (var r = p.executeQuery()) {
                while (r.next()) {
                    var account = new BirthdayPolicy.Account(
                        r.getInt(1),
                        r.getString(2),
                        r.getString(3),
                        r.getString(4),
                        r.getBoolean(5),
                        r.getString(6)
                    );
                    if (account.userId().equals(actor)) a = account;
                    if (account.userId().equals(target)) t = account;
                }
            }
        }
        if (!BirthdayPolicy.allowed(a, t)) throw new CodedException.Denied(ErrorCode.APP_141);
        return t;
    }

    private static void audit(
        Connection c,
        String actor,
        String target,
        String action,
        String ip,
        String agent
    ) throws SQLException {
        AuditLogLogic.recordStructured(
            c,
            actor,
            "PRIVACY",
            action,
            "USER",
            target,
            true,
            ip,
            agent,
            action,
            java.util.Map.of()
        );
    }

    private static void rollback(Connection c, Exception failure) {
        try {
            c.rollback();
        } catch (SQLException e) {
            failure.addSuppressed(e);
        }
    }
}
