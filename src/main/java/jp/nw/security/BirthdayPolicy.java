package jp.nw.security;

import java.time.LocalDate;
import jp.nw.domain.user.PermissionStatus;

/** Receives current DB values, never roles supplied by a request or stale session. */
public final class BirthdayPolicy {

    public record Account(
        int id,
        String userId,
        String permission,
        String type,
        boolean disabled,
        String deleted
    ) {}

    private BirthdayPolicy() {}

    public static boolean allowed(Account actor, Account target) {
        if (
            actor == null ||
            target == null ||
            actor.disabled() ||
            !"0".equals(actor.deleted()) ||
            !"0".equals(target.deleted()) ||
            "AI".equalsIgnoreCase(actor.type()) ||
            "AI".equalsIgnoreCase(target.type()) ||
            PermissionStatus.fromValue(actor.permission()).isEmpty() ||
            PermissionStatus.fromValue(target.permission()).isEmpty()
        ) return false;
        if (actor.id() == target.id()) return true;
        return (
            "0".equals(actor.permission()) ||
            ("1".equals(actor.permission()) && !"0".equals(target.permission()))
        );
    }

    public static LocalDate validate(LocalDate date) {
        if (
            date == null || date.isBefore(LocalDate.of(1900, 1, 1)) || date.isAfter(LocalDate.now())
        ) throw new jp.nw.model.CodedException.Validation(jp.nw.model.ErrorCode.APP_027);
        return date;
    }
}
