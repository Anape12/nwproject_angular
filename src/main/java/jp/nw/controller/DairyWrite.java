package jp.nw.controller;

import jp.nw.model.CodedException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.UserEntity;
import jp.nw.entity.WorkReportEntity;
import jp.nw.model.ApprovalLogic;
import jp.nw.model.WorkReportLogic;
import jp.nw.util.PageJson;

@WebServlet("/DairyWrite")
public class DairyWrite extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        UserEntity user = (UserEntity) session.getAttribute("loginUser");
        WorkReportLogic logic = new WorkReportLogic();
        String token = (String) session.getAttribute("reportCsrfToken");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("reportCsrfToken", token);
        }
        WorkReportEntity selected = parseReport(request.getParameter("edit"), user.getUserId(), logic);
        if (selected != null) {
            PageJson.set(request, "selectedReportJson", new ReportDetail(ReportRow.from(selected),
                    selected.isEditable(), selected.getBody(), selected.getReviewComment(),
                    selected.getReviewerName(), selected.getReviewedAtLabel()));
            PageJson.set(request, "attachmentsJson",
                    new jp.nw.model.AttachmentLogic().find("REPORT", String.valueOf(selected.getReportId())).stream()
                            .map(file -> new AttachmentRow(file.get("attachment_id").toString(),
                                    (String) file.get("original_name"))).toList());
        }
        LocalDate initialDate = LocalDate.now();
        try {
            if (request.getParameter("date") != null)
                initialDate = LocalDate.parse(request.getParameter("date"));
        } catch (Exception ignored) {
        }
        PageJson.set(request, "reportsJson", logic.findOwn(user.getUserId()).stream().map(ReportRow::from).toList());
        request.setAttribute("today", initialDate);
        request.setAttribute("csrfToken", token);
        request.setAttribute("flashMessage", session.getAttribute("reportFlash"));
        request.setAttribute("flashType", session.getAttribute("reportFlashType"));
        session.removeAttribute("reportFlash");
        session.removeAttribute("reportFlashType");
        request.getRequestDispatcher("/WEB-INF/jsp/report/reportWrite.jsp").forward(request, response);
    }

    private record ReportRow(String id, String title, String date, String updatedAtLabel, String status,
            String statusLabel) {
        static ReportRow from(WorkReportEntity report) {
            return new ReportRow(Long.toString(report.getReportId()), report.getTitle(), report.getReportDateValue(),
                    report.getUpdatedAtLabel(), report.getStatus(), report.getStatusLabel());
        }
    }

    private record ReportDetail(ReportRow report, boolean editable, String body, String reviewComment,
            String reviewerName, String reviewedAtLabel) {
    }

    private record AttachmentRow(String id, String name) {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        UserEntity user = (UserEntity) session.getAttribute("loginUser");
        if (!Objects.equals(session.getAttribute("reportCsrfToken"), request.getParameter("csrfToken"))) {
            response.sendError(403, jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_121));
            return;
        }
        WorkReportLogic logic = new WorkReportLogic();
        try {
            String action = request.getParameter("action");
            if ("submit".equals(action)) {
                new ApprovalLogic().submit("REPORT", id(request), user.getUserId());
                flash(session, "報告書を承認申請しました。", "success");
            } else if ("delete".equals(action)) {
                if (!logic.delete(id(request), user.getUserId()))
                    throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_007);
                flash(session, "下書きを削除しました。", "success");
            } else {
                WorkReportEntity report = build(request, user.getUserId());
                if ("update".equals(action)) {
                    report.setReportId(id(request));
                    if (!logic.update(report))
                        throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_008);
                    flash(session, "下書きを更新しました。", "success");
                } else {
                    logic.create(report);
                    flash(session, "下書きを保存しました。", "success");
                }
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            flash(session, jp.nw.model.ErrorMessageLogic.forDisplay(e), "error");
        }
        response.sendRedirect(request.getContextPath() + "/DairyWrite");
    }

    private WorkReportEntity build(HttpServletRequest request, String userId) {
        String title = trim(request.getParameter("title"));
        String body = trim(request.getParameter("body"));
        LocalDate date = LocalDate.parse(request.getParameter("reportDate"));
        if (title.isBlank() || title.length() > 150)
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_009);
        if (body.isBlank() || body.length() > 10000)
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_010);
        return WorkReportEntity.builder().authorId(userId).reportDate(date).title(title).body(body).build();
    }

    private long id(HttpServletRequest request) {
        try {
            return Long.parseLong(request.getParameter("reportId"));
        } catch (Exception e) {
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_011);
        }
    }

    private WorkReportEntity parseReport(String value, String userId, WorkReportLogic logic) {
        if (value == null)
            return null;
        try {
            return logic.findOwnById(Long.parseLong(value), userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void flash(HttpSession session, String message, String type) {
        session.setAttribute("reportFlash", message);
        session.setAttribute("reportFlashType", type);
    }
}
