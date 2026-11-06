package jp.nw.controller;

import java.io.IOException;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.nw.entity.AuditLogEntity;
import jp.nw.entity.UserEntity;
import jp.nw.domain.user.PermissionAction;
import jp.nw.model.AuditLogLogic;
import jp.nw.util.PermissionCheckUtil;

@WebServlet("/AuditLog")
public class AuditLogController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();

    protected void doGet(HttpServletRequest q, HttpServletResponse s) throws ServletException, IOException {
        UserEntity u = (UserEntity) q.getSession().getAttribute("loginUser");
        if (!PermissionCheckUtil.can(u, PermissionAction.AUDIT_VIEW)) {
            s.sendError(403);
            return;
        }
        if (!"json".equals(q.getParameter("format"))) {
            q.getRequestDispatcher("/WEB-INF/jsp/security/auditLog.jsp").forward(q, s);
            return;
        }

        String user = trim(q.getParameter("userId")), category = trim(q.getParameter("category")),
                action = trim(q.getParameter("action"));
        Boolean success = "true".equals(q.getParameter("success")) ? Boolean.TRUE
                : "false".equals(q.getParameter("success")) ? Boolean.FALSE : null;
        LocalDate from = date(q.getParameter("from")), to = date(q.getParameter("to"));
        s.setContentType("application/json");
        s.setCharacterEncoding("UTF-8");
        JSON.writeValue(s.getWriter(), AuditLogLogic.search(user, category, action, success, from, to, 500)
                .stream().map(AuditLogRow::from).toList());
    }

    private record AuditLogRow(String createdAt, String eventCategory, String eventAction,
            String actorUserId, String targetType, String targetId, boolean success, String ipAddress,
            String displayDetail) {
        static AuditLogRow from(AuditLogEntity log) {
            return new AuditLogRow(log.getCreatedAt() == null ? null : log.getCreatedAt().toString(),
                    log.getEventCategory(), log.getEventAction(), log.getActorUserId(), log.getTargetType(),
                    log.getTargetId(), log.isSuccess(), log.getIpAddress(), log.getDisplayDetail());
        }
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }

    private LocalDate date(String v) {
        try {
            return v == null || v.isBlank() ? null : LocalDate.parse(v);
        } catch (Exception e) {
            return null;
        }
    }
}
