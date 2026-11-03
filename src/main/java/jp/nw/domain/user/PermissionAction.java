package jp.nw.domain.user;

/** Operations authorized by role. Resource ownership is checked separately. */
public enum PermissionAction {
    DESIGN_REVERSE_VIEW,
    USER_MANAGE,
    AUDIT_VIEW,
    AI_CHARACTER_MANAGE,
    APPROVAL_REVIEW,
    APPROVAL_CONFIGURE,
    ATTENDANCE_CLOSE,
    CHAT_MEMBERS_MANAGE_ANY,
    THREAD_MODERATE_ANY,
    ADMIN_DASHBOARD_VIEW,
    EXTERNAL_TOOLS_VIEW,
    SEARCH_USER_INFO
}
