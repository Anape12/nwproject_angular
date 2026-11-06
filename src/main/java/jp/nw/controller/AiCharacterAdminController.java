package jp.nw.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.AiCharacterEntity;
import jp.nw.entity.UserEntity;
import jp.nw.domain.user.PermissionAction;
import jp.nw.model.AiCharacterLogic;
import jp.nw.util.PermissionCheckUtil;
import jp.nw.util.PageJson;

@WebServlet("/AiCharacterAdmin")
public class AiCharacterAdminController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest q, HttpServletResponse s) throws ServletException, IOException {
        UserEntity u = user(q);
        if (!PermissionCheckUtil.can(u, PermissionAction.AI_CHARACTER_MANAGE)) {
            s.sendError(403);
            return;
        }
        HttpSession session = q.getSession();
        if (session.getAttribute("aiAdminCsrf") == null)
            session.setAttribute("aiAdminCsrf", UUID.randomUUID().toString());
        AiCharacterLogic l = new AiCharacterLogic();
        PageJson.set(q, "charactersJson", l.findAll().stream().map(CharacterRow::from).toList());
        PageJson.set(q, "recentJobsJson", l.findRecentJobs().stream().map(JobRow::from).toList());
        String edit = q.getParameter("edit");
        if (edit != null)
            try {
                AiCharacterEntity selected = l.find(Long.parseLong(edit));
                if (selected != null) {
                    PageJson.set(q, "selectedJson", CharacterRow.from(selected));
                }
            } catch (NumberFormatException ignored) {
            }
        q.setAttribute("flash", session.getAttribute("aiAdminFlash"));
        q.setAttribute("flashType", session.getAttribute("aiAdminFlashType"));
        session.removeAttribute("aiAdminFlash");
        session.removeAttribute("aiAdminFlashType");
        q.getRequestDispatcher("/WEB-INF/jsp/ai/characterAdmin.jsp").forward(q, s);
    }

    protected void doPost(HttpServletRequest q, HttpServletResponse s) throws IOException {
        q.setCharacterEncoding("UTF-8");
        UserEntity u = user(q);
        HttpSession session = q.getSession(false);
        if (!PermissionCheckUtil.can(u, PermissionAction.AI_CHARACTER_MANAGE)) {
            s.sendError(403);
            return;
        }
        if (!Objects.equals(session.getAttribute("aiAdminCsrf"), q.getParameter("csrfToken"))) {
            s.sendError(403);
            return;
        }
        try {
            AiCharacterEntity v = AiCharacterEntity.builder().userId(trim(q.getParameter("userId")))
                    .characterName(trim(q.getParameter("characterName")))
                    .promptKey(trim(q.getParameter("promptKey"))).modelName(trim(q.getParameter("modelName")))
                    .replyMode(q.getParameter("replyMode")).activeFlg(q.getParameter("activeFlg") == null ? "0" : "1")
                    .build();
            String id = q.getParameter("characterId");
            AiCharacterLogic l = new AiCharacterLogic();
            if (id == null || id.isBlank())
                l.create(v);
            else {
                v.setCharacterId(Long.parseLong(id));
                l.update(v);
            }
            flash(session, "AI住人を保存しました。", "success");
        } catch (Exception e) {
            flash(session, jp.nw.model.ErrorMessageLogic.forDisplay(e), "error");
        }
        s.sendRedirect(q.getContextPath() + "/AiCharacterAdmin");
    }

    private UserEntity user(HttpServletRequest q) {
        HttpSession s = q.getSession(false);
        return s == null ? null : (UserEntity) s.getAttribute("loginUser");
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private void flash(HttpSession s, String m, String t) {
        s.setAttribute("aiAdminFlash", m);
        s.setAttribute("aiAdminFlashType", t);
    }

    private record CharacterRow(String id, String userId, String name, String promptKey, String modelName,
            String replyMode, boolean active) {
        static CharacterRow from(AiCharacterEntity character) {
            return new CharacterRow(Long.toString(character.getCharacterId()), character.getUserId(),
                    character.getCharacterName(), character.getPromptKey(), character.getModelName(),
                    character.getReplyMode(), "1".equals(character.getActiveFlg()));
        }
    }

    private record JobRow(String id, String characterName, String sourceType, String status, int retryCount,
            String errorMessage, String createdAt) {
        static JobRow from(Map<String, Object> job) {
            return new JobRow(job.get("jobId").toString(), (String) job.get("characterName"),
                    (String) job.get("sourceType"), (String) job.get("status"),
                    ((Number) job.get("retryCount")).intValue(), (String) job.get("errorMessage"),
                    Objects.toString(job.get("createdAt"), ""));
        }
    }
}
