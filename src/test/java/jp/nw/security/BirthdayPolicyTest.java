package jp.nw.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BirthdayPolicyTest {

    private BirthdayPolicy.Account user(int id, String role) {
        return new BirthdayPolicy.Account(id, "user" + id, role, "HUMAN", false, "0");
    }

    @Test
    void selfAndHierarchyAreEnforced() {
        assertTrue(BirthdayPolicy.allowed(user(1, "2"), user(1, "2")));
        assertFalse(BirthdayPolicy.allowed(user(1, "2"), user(2, "2")));
        assertTrue(BirthdayPolicy.allowed(user(1, "1"), user(2, "2")));
        assertFalse(BirthdayPolicy.allowed(user(1, "1"), user(2, "0")));
        assertTrue(BirthdayPolicy.allowed(user(1, "0"), user(2, "0")));
        assertFalse(BirthdayPolicy.allowed(null, user(1, "2")));
        assertFalse(BirthdayPolicy.allowed(user(1, "unknown"), user(1, "unknown")));
    }

    @Test
    void disabledDeletedAndAiActorsAreDenied() {
        for (var actor : new BirthdayPolicy.Account[] {
            new BirthdayPolicy.Account(1, "user1", "0", "HUMAN", true, "0"),
            new BirthdayPolicy.Account(1, "user1", "0", "HUMAN", false, "1"),
            new BirthdayPolicy.Account(1, "user1", "0", "AI", false, "0"),
        })
            assertFalse(BirthdayPolicy.allowed(actor, user(2, "2")));
    }

    @Test
    void invalidDatesAndSwappedOwnersAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            BirthdayPolicy.validate(LocalDate.now().plusDays(1))
        );
        assertThrows(IllegalArgumentException.class, () ->
            BirthdayPolicy.validate(LocalDate.of(1899, 12, 31))
        );
        String authenticated = "nwproject:users_info:1:birthday:v1\n1996-04-12";
        assertEquals(LocalDate.of(1996, 4, 12), BirthdayRepository.decode(1, authenticated));
        assertThrows(SecurityException.class, () -> BirthdayRepository.decode(2, authenticated));
    }
}
