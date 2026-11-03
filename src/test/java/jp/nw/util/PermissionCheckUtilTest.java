package jp.nw.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jp.nw.domain.user.PermissionAction;
import jp.nw.domain.user.PermissionStatus;
import jp.nw.entity.UserEntity;

class PermissionCheckUtilTest {
    private UserEntity user(String permission, String type) {
        return UserEntity.builder().permission(permission).accountType(type).build();
    }

    @Test
    void systemAdministratorCanUseEveryDeclaredAction() {
        UserEntity system = user("0", "HUMAN");
        for (PermissionAction action : PermissionAction.values())
            assertTrue(PermissionCheckUtil.can(system, action), action.name());
    }

    @Test
    void administratorCannotUseSystemOnlyActions() {
        UserEntity admin = user("1", "HUMAN");
        for (PermissionAction action : PermissionAction.values())
            if (action == PermissionAction.DESIGN_REVERSE_VIEW
                    || action == PermissionAction.SEARCH_USER_INFO)
                assertFalse(PermissionCheckUtil.can(admin, action));
            else
                assertTrue(PermissionCheckUtil.can(admin, action), action.name());
    }

    @Test
    void generalUnknownAiAndMissingUserAreDenied() {
        for (PermissionAction action : PermissionAction.values()) {
            assertFalse(PermissionCheckUtil.can(user("2", "HUMAN"), action));
            assertFalse(PermissionCheckUtil.can(user("9", "HUMAN"), action));
            assertFalse(PermissionCheckUtil.can(user("0", "AI"), action));
            assertFalse(PermissionCheckUtil.can((UserEntity) null, action));
        }
        assertFalse(PermissionCheckUtil.can(user("0", "HUMAN"), null));
        assertTrue(PermissionStatus.fromValue("0").isPresent());
        assertTrue(PermissionStatus.fromValue("9").isEmpty());
    }

    @Test
    void businessAdminCannotCreateOrManageSystemRole() {
        assertFalse(PermissionCheckUtil.canManageRole(user("1", "HUMAN"), "0"));
        assertTrue(PermissionCheckUtil.canManageRole(user("1", "HUMAN"), "1"));
        assertTrue(PermissionCheckUtil.canManageRole(user("1", "HUMAN"), "2"));
        assertTrue(PermissionCheckUtil.canManageRole(user("0", "HUMAN"), "0"));
        assertFalse(PermissionCheckUtil.canManageRole(user("0", "HUMAN"), "9"));
    }
}
