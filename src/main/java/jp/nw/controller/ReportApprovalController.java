package jp.nw.controller;

import jp.nw.model.CodedException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.ApprovalRequestEntity;
import jp.nw.entity.UserEntity;
import jp.nw.domain.user.PermissionAction;
import jp.nw.model.ApprovalLogic;
import jp.nw.util.PermissionCheckUtil;
import jp.nw.util.PageJson;
import jp.nw.util.AngularPage;

@WebServlet("/ReportApproval")
public class ReportApprovalController extends HttpServlet {
    protected void doGet(HttpServletRequest q, HttpServletResponse s) throws ServletException, IOException {
        HttpSession session = q.getSession();
        UserEntity u = (UserEntity) session.getAttribute("loginUser");
        if (!reviewer(u)) {
            s.sendError(403);
            return;
        }
        if (!"json".equals(q.getParameter("format"))) {
            AngularPage.serve(getServletContext(), s, "reports/report-approval");
            return;
        }
        ApprovalLogic l = new ApprovalLogic();
        String token = (String) session.getAttribute("approvalCsrfToken");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("approvalCsrfToken", token);
        }
        List<ApplicationRow> applications = l.findAll().stream().map(ApplicationRow::from).toList();
        boolean canConfigure = PermissionCheckUtil.can(u, PermissionAction.APPROVAL_CONFIGURE);
        ApprovalRequestEntity selected = selected(q.getParameter("id"), l);
        ApplicationDetail detail = selected == null ? null : new ApplicationDetail(ApplicationRow.from(selected),
                selected.getDetail(), selected.getReviewComment(), selected.getReviewerName(),
                selected.getReviewedAtLabel(), l.history(selected.getApprovalId()).stream()
                        .map(HistoryRow::from).toList());
        String flash = Objects.toString(session.getAttribute("approvalFlash"), "");
        String flashType = Objects.toString(session.getAttribute("approvalFlashType"), "");
        session.removeAttribute("approvalFlash");
        session.removeAttribute("approvalFlashType");
        PageJson.write(s, new ApprovalPage(token, flash, flashType, canConfigure, applications, detail));
    }

    private record ApprovalPage(String csrfToken, String flash, String flashType, boolean canConfigure,
            List<ApplicationRow> applications, ApplicationDetail selected) {
    }

    protected void doPost(HttpServletRequest q, HttpServletResponse s) throws IOException {
        q.setCharacterEncoding("UTF-8");
        HttpSession session = q.getSession(false);
        UserEntity u = (UserEntity) session.getAttribute("loginUser");
        if (!reviewer(u)) {
            s.sendError(403);
            return;
        }
        if (!Objects.equals(session.getAttribute("approvalCsrfToken"), q.getParameter("csrfToken"))) {
            s.sendError(403);
            return;
        }
        try {
            ApprovalLogic l = new ApprovalLogic();
            String action = q.getParameter("action");
            if ("route".equals(action) && PermissionCheckUtil.can(u, PermissionAction.APPROVAL_CONFIGURE))
                l.saveRoute(q.getParameter("applicationType"), Integer.parseInt(q.getParameter("requiredSteps")));
            else if ("delegate".equals(action) && PermissionCheckUtil.can(u, PermissionAction.APPROVAL_CONFIGURE))
                l.addDelegate(u.getUserId(), q.getParameter("delegateUserId"),
                        LocalDate.parse(q.getParameter("validFrom")), LocalDate.parse(q.getParameter("validTo")));
            else if ("route".equals(action) || "delegate".equals(action))
                throw new CodedException.Denied(jp.nw.model.ErrorCode.APP_020);
            else {
                String decision = q.getParameter("decision"), comment = trim(q.getParameter("comment"));
                if ("REJECTED".equals(decision) && comment.isBlank())
                    throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_021);
                String[] values = q.getParameterValues("approvalIds");
                if (values != null) {
                    List<Long> ids = new ArrayList<>();
                    for (String v : values)
                        ids.add(Long.valueOf(v));
                    l.reviewBatch(ids, u, decision, comment);
                } else
                    l.review(Long.parseLong(q.getParameter("approvalId")), u, decision, comment);
            }
            flash(session, "処理が完了しました。", "success");
        } catch (Exception e) {
            flash(session, jp.nw.model.ErrorMessageLogic.forDisplay(e), "error");
        }
        s.sendRedirect(q.getContextPath() + "/ReportApproval");
    }

    private ApprovalRequestEntity selected(String v, ApprovalLogic l) {
        try {
            return v == null ? null : l.findById(Long.parseLong(v));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean reviewer(UserEntity u) {
        return new ApprovalLogic().canReview(u);
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }

    private void flash(HttpSession s, String m, String t) {
        s.setAttribute("approvalFlash", m);
        s.setAttribute("approvalFlashType", t);
    }

    private record ApplicationRow(String id, String title, String typeLabel, String applicantName, String targetDate,
            String status, String statusLabel) {
        static ApplicationRow from(ApprovalRequestEntity item) {
            return new ApplicationRow(Long.toString(item.getApprovalId()), item.getTitle(), item.getTypeLabel(),
                    item.getApplicantName(), Objects.toString(item.getTargetDate(), ""), item.getStatus(),
                    item.getStatusLabel());
        }
    }

    private record ApplicationDetail(ApplicationRow application, String detail, String reviewComment,
            String reviewerName, String reviewedAtLabel, List<HistoryRow> history) {
    }

    private record HistoryRow(String action, String actor, String actedAt, String comment) {
        static HistoryRow from(Map<String, Object> history) {
            return new HistoryRow((String) history.get("action"), (String) history.get("acted_by"),
                    Objects.toString(history.get("acted_at"), ""), (String) history.get("comment"));
        }
    }
}
