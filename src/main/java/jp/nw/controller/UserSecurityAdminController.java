package jp.nw.controller;

import jp.nw.model.CodedException;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.UserEntity;
import jp.nw.domain.user.PermissionAction;
import jp.nw.domain.user.PermissionStatus;
import jp.nw.model.AuditLogLogic;
import jp.nw.model.UserSecurityLogic;
import jp.nw.util.PermissionCheckUtil;
import jp.nw.util.PageJson;

@WebServlet("/UserSecurityAdmin")
public class UserSecurityAdminController extends HttpServlet {
    protected void doGet(HttpServletRequest q, HttpServletResponse s) throws ServletException, IOException {
        HttpSession session = q.getSession();
        UserEntity u = user(session);
        if (!admin(u)) {
            s.sendError(403);
            return;
        }
        String token = (String) session.getAttribute("userSecurityCsrf");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("userSecurityCsrf", token);
        }
        q.setAttribute("csrfToken", token);
        boolean canManageSystemUsers = PermissionCheckUtil.hasRole(u, PermissionStatus.SYSTEM_ADMINISTRATOR);
        q.setAttribute("canManageSystemUsers", canManageSystemUsers);
        PageJson.set(q, "usersJson", new UserSecurityLogic().findAll().stream()
                .map(account -> AccountRow.from(account, canManageSystemUsers)).toList());
        q.setAttribute("flash", session.getAttribute("securityFlash"));
        q.setAttribute("flashType", session.getAttribute("securityFlashType"));
        session.removeAttribute("securityFlash");
        session.removeAttribute("securityFlashType");
        q.getRequestDispatcher("/WEB-INF/jsp/security/userSecurityAdmin.jsp").forward(q, s);
    }

    protected void doPost(HttpServletRequest q, HttpServletResponse s) throws IOException {
        q.setCharacterEncoding("UTF-8");
        HttpSession session = q.getSession(false);
        UserEntity u = user(session);
        if (!admin(u)) {
            s.sendError(403);
            return;
        }
        if (!Objects.equals(session.getAttribute("userSecurityCsrf"), q.getParameter("csrfToken"))) {
            s.sendError(403);
            return;
        }
        String target = trim(q.getParameter("targetUserId")), action = q.getParameter("action");
        try {
            UserSecurityLogic l = new UserSecurityLogic();
            String ip = AuditLogLogic.clientIp(q), agent = q.getHeader("User-Agent");
            switch (action) {
                case "disable" -> l.setDisabled(u.getUserId(), target, true, ip, agent);
                case "enable" -> l.setDisabled(u.getUserId(), target, false, ip, agent);
                case "unlock" -> l.unlock(u.getUserId(), target, ip, agent);
                case "resetPassword" ->
                    l.resetPassword(u.getUserId(), target, q.getParameter("temporaryPassword"), ip, agent);
                case "updateProfile" -> l.updateProfile(u.getUserId(), target, trim(q.getParameter("firstName")),
                        trim(q.getParameter("lastName")), q.getParameter("permission"), ip, agent);
                default -> throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_029);
            }
            flash(session, "更新しました。", "success");
        } catch (Exception e) {
            AuditLogLogic.record(q, "SECURITY", action == null ? "UNKNOWN" : action.toUpperCase(), "USER", target,
                    false, e.getMessage());
            flash(session, jp.nw.model.ErrorMessageLogic.forDisplay(e), "error");
        }
        s.sendRedirect(q.getContextPath() + "/UserSecurityAdmin");
    }

    private UserEntity user(HttpSession s) {
        return s == null ? null : (UserEntity) s.getAttribute("loginUser");
    }

    private boolean admin(UserEntity u) {
        return PermissionCheckUtil.can(u, PermissionAction.USER_MANAGE);
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }

    private void flash(HttpSession s, String m, String t) {
        s.setAttribute("securityFlash", m);
        s.setAttribute("securityFlashType", t);
    }

    private record AccountRow(String userId, String firstName, String lastName, String accountType, String permission,
            boolean disabled, String lastLoginAt, String lockedUntil, int failedLoginCount,
            boolean forcePasswordChange, boolean manageable) {
        static AccountRow from(UserEntity user, boolean canManageSystemUsers) {
            return new AccountRow(user.getUserId(), user.getFirstName(), user.getLastName(), user.getAccountType(),
                    user.getPermission(), user.isAccountDisabled(), Objects.toString(user.getLastLoginAt(), null),
                    Objects.toString(user.getLockedUntil(), null), user.getFailedLoginCount(),
                    user.isForcePasswordChange(),
                    !"AI".equals(user.getAccountType()) && (!"0".equals(user.getPermission()) || canManageSystemUsers));
        }
    }
}
