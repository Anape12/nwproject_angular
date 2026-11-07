package jp.nw.controller;

import jp.nw.model.CodedException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.AttendanceEntity;
import jp.nw.entity.UserEntity;
import jp.nw.domain.user.PermissionAction;
import jp.nw.util.PermissionCheckUtil;
import jp.nw.util.PageJson;
import jp.nw.util.AngularPage;
import jp.nw.model.AttendanceDay;
import jp.nw.model.AttendanceLogic;
import jp.nw.model.WorkReportLogic;

@WebServlet("/WorkManagement")
public class WorkManagement extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Set<String> WORK_TYPES = Set.of("OFFICE", "REMOTE", "LEAVE", "HOLIDAY");
    private static final Set<String> ATTENDANCE_TYPES = Set.of("NORMAL", "LATE", "EARLY", "ABSENT", "PAID_LEAVE",
            "COMP_LEAVE", "HOLIDAY_WORK");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"json".equals(request.getParameter("format"))) {
            AngularPage.serve(getServletContext(), response, "attendance/attendance");
            return;
        }
        HttpSession session = request.getSession();
        UserEntity user = (UserEntity) session.getAttribute("loginUser");
        YearMonth month = month(request);
        AttendanceLogic logic = new AttendanceLogic();
        List<AttendanceEntity> records = logic.findMonth(user.getUserId(), month);
        Map<LocalDate, AttendanceEntity> byDate = new HashMap<>();
        for (AttendanceEntity record : records)
            byDate.put(record.getWorkDate(), record);
        List<AttendanceDay> days = new ArrayList<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            days.add(new AttendanceDay(date, byDate.get(date)));
        }
        AttendanceEntity selected = selected(request.getParameter("edit"), user.getUserId(), logic);
        LocalDate selectedDate = parseDate(request.getParameter("date"), LocalDate.now());
        String token = (String) session.getAttribute("attendanceCsrfToken");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("attendanceCsrfToken", token);
        }
        YearMonth previous = month.minusMonths(1);
        YearMonth next = month.plusMonths(1);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("year", month.getYear());
        data.put("month", month.getMonthValue());
        data.put("previousYear", previous.getYear());
        data.put("previousMonth", previous.getMonthValue());
        data.put("nextYear", next.getYear());
        data.put("nextMonth", next.getMonthValue());
        data.put("selectedDate", selectedDate.toString());
        data.put("days", days.stream().map(DayRow::from).toList());
        data.put("canClose", PermissionCheckUtil.can(user, PermissionAction.ATTENDANCE_CLOSE));
        data.put("summary", logic.summary(user.getUserId(), month));
        data.put("closed", logic.isClosed(user.getUserId(), month));
        data.put("monthKey", month.toString());
        if (selected != null) {
            data.put("selected", AttendanceRow.from(selected));
        }
        data.put("reports", new WorkReportLogic().findOwn(user.getUserId()).stream()
                .map(report -> new ReportOption(Long.toString(report.getReportId()), report.getReportDateValue(),
                        report.getTitle(), report.getStatusLabel())).toList());
        data.put("csrfToken", token);
        data.put("flash", session.getAttribute("attendanceFlash"));
        data.put("flashType", session.getAttribute("attendanceFlashType"));
        session.removeAttribute("attendanceFlash");
        session.removeAttribute("attendanceFlashType");
        PageJson.write(response, data);
    }

    private record DayRow(String date, int number, String weekday, boolean today, boolean weekend,
            AttendanceRow attendance) {
        static DayRow from(AttendanceDay day) {
            return new DayRow(day.getDateValue(), day.getDay(), day.getWeekday(), day.isToday(), day.isWeekend(),
                    day.getAttendance() == null ? null : AttendanceRow.from(day.getAttendance()));
        }
    }

    private record AttendanceRow(String id, String date, String workType, String workTypeLabel, String attendanceType,
            String clockIn, String clockOut, int breakMinutes, String workingTime, String reportId, String reportTitle,
            String approvalStatus, String approvalStatusLabel, boolean editable, String note) {
        static AttendanceRow from(AttendanceEntity item) {
            return new AttendanceRow(Long.toString(item.getAttendanceId()), item.getWorkDate().toString(),
                    item.getWorkType(), item.getWorkTypeLabel(), item.getAttendanceType(),
                    item.getClockInValue(), item.getClockOutValue(), item.getBreakMinutes(), item.getWorkingTimeLabel(),
                    Objects.toString(item.getReportId(), ""), item.getReportTitle(), item.getApprovalStatus(),
                    item.getApprovalStatusLabel(), item.isEditable(), item.getNote());
        }
    }

    private record ReportOption(String id, String date, String title, String statusLabel) {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        UserEntity user = (UserEntity) session.getAttribute("loginUser");
        YearMonth month = month(request);
        if (!Objects.equals(session.getAttribute("attendanceCsrfToken"), request.getParameter("csrfToken"))) {
            response.sendError(403, jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_121));
            return;
        }
        AttendanceLogic logic = new AttendanceLogic();
        try {
            if ("submitMonth".equals(request.getParameter("action"))) {
                int count = new jp.nw.model.ApprovalLogic().submitAttendanceMonth(user.getUserId(), month);
                flash(session, month.getMonthValue() + "月の勤怠を" + count + "件一括申請しました。", "success");
            } else if ("submit".equals(request.getParameter("action"))) {
                new jp.nw.model.ApprovalLogic().submit("ATTENDANCE", id(request), user.getUserId());
                flash(session, "勤怠を日付単位で承認申請しました。", "success");
            } else if ("delete".equals(request.getParameter("action"))) {
                if (!logic.delete(id(request), user.getUserId()))
                    throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_030);
                flash(session, "勤怠情報を削除しました。", "success");
            } else {
                AttendanceEntity value = build(request, user.getUserId());
                if ("update".equals(request.getParameter("action")))
                    value.setAttendanceId(id(request));
                logic.save(value);
                flash(session, "勤怠情報を保存しました。", "success");
            }
        } catch (IllegalArgumentException e) {
            flash(session, jp.nw.model.ErrorMessageLogic.forDisplay(e), "error");
        }
        response.sendRedirect(request.getContextPath() + "/WorkManagement?year=" + month.getYear() + "&month="
                + month.getMonthValue());
    }

    private AttendanceEntity build(HttpServletRequest request, String userId) {
        LocalDate date = LocalDate.parse(request.getParameter("workDate"));
        String type = request.getParameter("workType");
        if (!WORK_TYPES.contains(type))
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_031);
        boolean nonWorking = "LEAVE".equals(type) || "HOLIDAY".equals(type);
        LocalTime in = null, out = null;
        int breakMinutes = 0;
        if (!nonWorking) {
            in = LocalTime.parse(request.getParameter("clockIn"));
            out = LocalTime.parse(request.getParameter("clockOut"));
            if (!out.isAfter(in))
                throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_032);
            try {
                breakMinutes = Integer.parseInt(request.getParameter("breakMinutes"));
            } catch (Exception e) {
                throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_033);
            }
            long duration = java.time.temporal.ChronoUnit.MINUTES.between(in, out);
            if (breakMinutes < 0 || breakMinutes >= duration)
                throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_034);
        }
        String note = trim(request.getParameter("note"));
        if (note.length() > 500)
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_035);
        String reportValue = request.getParameter("reportId");
        Long reportId = reportValue == null || reportValue.isBlank() ? null : Long.valueOf(reportValue);
        String attendanceType = request.getParameter("attendanceType");
        if (!ATTENDANCE_TYPES.contains(attendanceType))
            attendanceType = "NORMAL";
        int overtime = 0;
        if (in != null && out != null)
            overtime = (int) Math.max(0, java.time.temporal.ChronoUnit.MINUTES.between(in, out) - breakMinutes - 480);
        return AttendanceEntity.builder().userId(userId).workDate(date).clockIn(in).clockOut(out)
                .breakMinutes(breakMinutes).workType(type).attendanceType(attendanceType).overtimeMinutes(overtime)
                .note(note).reportId(reportId).build();
    }

    private YearMonth month(HttpServletRequest request) {
        try {
            String y = request.getParameter("year"), m = request.getParameter("month");
            return y == null || m == null ? YearMonth.now() : YearMonth.of(Integer.parseInt(y), Integer.parseInt(m));
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return value == null ? fallback : LocalDate.parse(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private AttendanceEntity selected(String value, String userId, AttendanceLogic logic) {
        if (value == null)
            return null;
        try {
            return logic.findById(Long.parseLong(value), userId);
        } catch (Exception e) {
            return null;
        }
    }

    private long id(HttpServletRequest request) {
        try {
            return Long.parseLong(request.getParameter("attendanceId"));
        } catch (Exception e) {
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_036);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void flash(HttpSession session, String message, String type) {
        session.setAttribute("attendanceFlash", message);
        session.setAttribute("attendanceFlashType", type);
    }
}
