package jp.nw.util;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import jp.nw.domain.user.PermissionAction;
import jp.nw.domain.user.PermissionStatus;
import jp.nw.entity.UserEntity;

/**
 * Central, database-free role-to-operation policy. Unknown roles are denied.
 */
public final class PermissionCheckUtil {
        private static final Map<PermissionAction, EnumSet<PermissionStatus>> ALLOWED = new EnumMap<>(
                        PermissionAction.class);

        static {
                ALLOWED.put(PermissionAction.DESIGN_REVERSE_VIEW, EnumSet.of(PermissionStatus.SYSTEM_ADMINISTRATOR));
                EnumSet<PermissionStatus> managers = EnumSet.of(
                                PermissionStatus.SYSTEM_ADMINISTRATOR, PermissionStatus.ADMINISTRATOR);
                ALLOWED.put(PermissionAction.USER_MANAGE, EnumSet.copyOf(managers));
                ALLOWED.put(PermissionAction.AUDIT_VIEW, EnumSet.copyOf(managers));
                ALLOWED.put(PermissionAction.AI_CHARACTER_MANAGE, EnumSet.copyOf(managers));
                ALLOWED.put(PermissionAction.APPROVAL_REVIEW, EnumSet.copyOf(managers));
                ALLOWED.put(PermissionAction.APPROVAL_CONFIGURE, EnumSet.copyOf(managers));
                ALLOWED.put(PermissionAction.ATTENDANCE_CLOSE, EnumSet.copyOf(managers));
                ALLOWED.put(PermissionAction.CHAT_MEMBERS_MANAGE_ANY, EnumSet.copyOf(managers));
                ALLOWED.put(PermissionAction.THREAD_MODERATE_ANY, EnumSet.copyOf(managers));
                ALLOWED.put(PermissionAction.ADMIN_DASHBOARD_VIEW, EnumSet.copyOf(managers));
                ALLOWED.put(PermissionAction.EXTERNAL_TOOLS_VIEW, EnumSet.copyOf(managers));
        }

        private PermissionCheckUtil() {
        }

        public static boolean can(UserEntity user, PermissionAction action) {
                return user != null && !"AI".equalsIgnoreCase(user.getAccountType())
                                && canRole(user.getPermission(), action);
        }

        private static boolean canRole(String permission, PermissionAction action) {
                if (action == null)
                        return false;
                return PermissionStatus.fromValue(permission)
                                .map(role -> role == PermissionStatus.SYSTEM_ADMINISTRATOR
                                                || ALLOWED.getOrDefault(action, EnumSet.noneOf(PermissionStatus.class))
                                                                .contains(role))
                                .orElse(false);
        }

        public static boolean hasRole(UserEntity user, PermissionStatus role) {
                return user != null && role != null && !"AI".equalsIgnoreCase(user.getAccountType())
                                && role.getValue().equals(user.getPermission());
        }

        /**
         * A business administrator must never create or modify a system administrator.
         */
        public static boolean canManageRole(UserEntity actor, String targetPermission) {
                if (!can(actor, PermissionAction.USER_MANAGE))
                        return false;
                return PermissionStatus.fromValue(targetPermission)
                                .map(role -> role != PermissionStatus.SYSTEM_ADMINISTRATOR
                                                || hasRole(actor, PermissionStatus.SYSTEM_ADMINISTRATOR))
                                .orElse(false);
        }
}
