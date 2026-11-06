package jp.nw.controller;

import jp.nw.model.CodedException;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.ScheduleEventEntity;
import jp.nw.entity.UserEntity;
import jp.nw.model.ScheduleCalendarDay;
import jp.nw.model.ScheduleEventLogic;
import jp.nw.util.PageJson;

@WebServlet("/OpenCalender")
public class OpenCalender extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Set<String> COLORS = Set.of("#1a73e8", "#188038", "#d93025", "#9334e6", "#f9ab00", "#5f6368");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        UserEntity loginUser = (UserEntity) session.getAttribute("loginUser");
        YearMonth month = parseMonth(request);
        LocalDate selectedDate = parseDate(request.getParameter("date"), LocalDate.now());
        ScheduleEventLogic logic = new ScheduleEventLogic();

        LocalDate gridStart = previousOrSameSunday(month.atDay(1));
        LocalDate gridEnd = nextOrSameSaturday(month.atEndOfMonth());
        List<ScheduleEventEntity> events = logic.findInRange(loginUser.getUserId(), gridStart.atStartOfDay(), gridEnd.plusDays(1).atStartOfDay());
        ScheduleEventEntity editEvent = findEditEvent(request.getParameter("edit"), loginUser.getUserId(), logic);

        String csrfToken = (String) session.getAttribute("scheduleCsrfToken");
        if (csrfToken == null) {
            csrfToken = UUID.randomUUID().toString();
            session.setAttribute("scheduleCsrfToken", csrfToken);
        }

        PageJson.set(request, "daysJson", createDays(month, gridStart, gridEnd, events).stream()
                .map(DayRow::from).toList());
        request.setAttribute("displayMonth", month);
        request.setAttribute("previousMonth", month.minusMonths(1));
        request.setAttribute("nextMonth", month.plusMonths(1));
        request.setAttribute("selectedDate", selectedDate);
        if (editEvent != null) {
            request.setAttribute("eventOwner", loginUser.getUserId().equals(editEvent.getUserId()));
            request.setAttribute("eventParticipant", logic.isParticipant(editEvent.getEventId(), loginUser.getUserId()));
            PageJson.set(request, "editEventJson", EditRow.from(editEvent));
            PageJson.set(request, "attachmentsJson", new jp.nw.model.AttachmentLogic()
                    .find("SCHEDULE", Long.toString(editEvent.getEventId())).stream()
                    .map(file -> new AttachmentRow(file.get("attachment_id").toString(),
                            (String) file.get("original_name"))).toList());
        }
        PageJson.set(request, "scheduleUsersJson", logic.users(loginUser.getUserId()));
        request.setAttribute("flashMessage", session.getAttribute("scheduleFlash"));
        request.setAttribute("flashType", session.getAttribute("scheduleFlashType"));
        request.setAttribute("csrfToken", csrfToken);
        session.removeAttribute("scheduleFlash");
        session.removeAttribute("scheduleFlashType");
        request.getRequestDispatcher("/WEB-INF/jsp/calender/calender.jsp").forward(request, response);
    }

    private record DayRow(String date, int number, boolean currentMonth, boolean today, List<EventRow> events) {
        static DayRow from(ScheduleCalendarDay day) {
            return new DayRow(day.getDateValue(), day.getDayOfMonth(), day.isCurrentMonth(), day.isToday(),
                    day.getEvents().stream().map(EventRow::from).toList());
        }
    }

    private record EventRow(String id, String title, String color, String time) {
        static EventRow from(ScheduleEventEntity event) {
            return new EventRow(Long.toString(event.getEventId()), event.getTitle(), event.getColor(),
                    event.getTimeLabel());
        }
    }

    private record EditRow(String id, String title, boolean allDay, String startDate, String endDate,
            String startTime, String endTime, String color, String visibility, String recurrence,
            String recurrenceUntil, String description) {
        static EditRow from(ScheduleEventEntity event) {
            return new EditRow(Long.toString(event.getEventId()), event.getTitle(), event.isAllDay(),
                    event.getStartDateValue(), event.getEndDateValue(), event.getStartTimeValue(),
                    event.getEndTimeValue(), event.getColor(), event.getVisibility(), event.getRecurrenceRule(),
                    Objects.toString(event.getRecurrenceUntil(), ""), event.getDescription());
        }
    }

    private record AttachmentRow(String id, String name) {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        UserEntity loginUser = (UserEntity) session.getAttribute("loginUser");
        YearMonth month = parseMonth(request);

        if (!Objects.equals(session.getAttribute("scheduleCsrfToken"), request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_121));
            return;
        }

        ScheduleEventLogic logic = new ScheduleEventLogic();
        try {
            String action = request.getParameter("action");
            if ("delete".equals(action)) {
                if (!logic.delete(parseRequiredId(request.getParameter("eventId")), loginUser.getUserId())) {
                    throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_012);
                }
                setFlash(session, "予定を削除しました。", "success");
            } else {
                ScheduleEventEntity event = buildEvent(request, loginUser.getUserId());
                if ("update".equals(action)) {
                    event.setEventId(parseRequiredId(request.getParameter("eventId")));
                    if (!logic.update(event)) throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_013);
                    setFlash(session, "予定を更新しました。", "success");
                } else {
                    int count=createRecurring(logic,event,request.getParameterValues("participantIds"));
                    setFlash(session, count==1?"予定を登録しました。":"繰り返し予定を"+count+"件登録しました。", "success");
                }
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            setFlash(session, jp.nw.model.ErrorMessageLogic.forDisplay(e), "error");
        }
        response.sendRedirect(request.getContextPath() + "/OpenCalender?year=" + month.getYear() + "&month=" + month.getMonthValue());
    }

    private ScheduleEventEntity buildEvent(HttpServletRequest request, String userId) {
        String title = trim(request.getParameter("title"));
        String description = trim(request.getParameter("description"));
        boolean allDay = "true".equals(request.getParameter("allDay"));
        LocalDate startDate = LocalDate.parse(request.getParameter("startDate"));
        LocalDate endDate = LocalDate.parse(request.getParameter("endDate"));
        if (title.isBlank() || title.length() > 100) throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_014);
        if (description.length() > 1000) throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_015);
        if (endDate.isBefore(startDate)) throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_016);

        LocalDateTime startAt;
        LocalDateTime endAt;
        if (allDay) {
            startAt = startDate.atStartOfDay();
            endAt = endDate.plusDays(1).atStartOfDay();
        } else {
            startAt = LocalDateTime.of(startDate, LocalTime.parse(request.getParameter("startTime")));
            endAt = LocalDateTime.of(endDate, LocalTime.parse(request.getParameter("endTime")));
            if (!endAt.isAfter(startAt)) throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_017);
        }
        String color = request.getParameter("color");
        if (!COLORS.contains(color)) color = "#1a73e8";
        String visibility="SHARED".equals(request.getParameter("visibility"))?"SHARED":"PRIVATE";String recurrence=Set.of("DAILY","WEEKLY","MONTHLY").contains(request.getParameter("recurrence"))?request.getParameter("recurrence"):null;LocalDate until=recurrence==null?null:parseDate(request.getParameter("recurrenceUntil"),startDate);if(until!=null&&until.isBefore(startDate))throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_018);
        return ScheduleEventEntity.builder().userId(userId).title(title).description(description)
                .startAt(startAt).endAt(endAt).allDay(allDay).color(color).visibility(visibility).recurrenceRule(recurrence).recurrenceUntil(until).build();
    }

    private int createRecurring(ScheduleEventLogic logic,ScheduleEventEntity source,String[] participants){int count=0;LocalDateTime start=source.getStartAt(),end=source.getEndAt();while(true){ScheduleEventEntity e=ScheduleEventEntity.builder().userId(source.getUserId()).title(source.getTitle()).description(source.getDescription()).startAt(start).endAt(end).allDay(source.isAllDay()).color(source.getColor()).visibility(source.getVisibility()).recurrenceRule(source.getRecurrenceRule()).recurrenceUntil(source.getRecurrenceUntil()).build();long eventId=logic.create(e);logic.invite(eventId,participants);count++;if(source.getRecurrenceRule()==null||source.getRecurrenceUntil()==null||count>=366)break;LocalDateTime nextStart=switch(source.getRecurrenceRule()){case "DAILY"->start.plusDays(1);case "WEEKLY"->start.plusWeeks(1);default->start.plusMonths(1);};LocalDateTime nextEnd=end.plus(java.time.Duration.between(start,nextStart));if(nextStart.toLocalDate().isAfter(source.getRecurrenceUntil()))break;start=nextStart;end=nextEnd;}return count;}

    private List<ScheduleCalendarDay> createDays(YearMonth month, LocalDate start, LocalDate end, List<ScheduleEventEntity> events) {
        List<ScheduleCalendarDay> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            ScheduleCalendarDay day = new ScheduleCalendarDay(date, YearMonth.from(date).equals(month), date.equals(LocalDate.now()));
            for (ScheduleEventEntity event : events) {
                if (!date.isBefore(event.getStartAt().toLocalDate()) && !date.isAfter(event.getLastDisplayDate())) day.getEvents().add(event);
            }
            days.add(day);
        }
        return days;
    }

    private LocalDate previousOrSameSunday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) date = date.minusDays(1);
        return date;
    }

    private LocalDate nextOrSameSaturday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.SATURDAY) date = date.plusDays(1);
        return date;
    }

    private ScheduleEventEntity findEditEvent(String value, String userId, ScheduleEventLogic logic) {
        if (value == null || value.isBlank()) return null;
        try { return logic.findById(Long.parseLong(value), userId); } catch (NumberFormatException e) { return null; }
    }

    private long parseRequiredId(String value) {
        try { return Long.parseLong(value); } catch (NumberFormatException e) { throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_019); }
    }

    private YearMonth parseMonth(HttpServletRequest request) {
        try {
            String year = request.getParameter("year"); String month = request.getParameter("month");
            return year == null || month == null ? YearMonth.now() : YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
        } catch (RuntimeException e) { return YearMonth.now(); }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        try { return value == null ? fallback : LocalDate.parse(value); } catch (DateTimeParseException e) { return fallback; }
    }

    private String trim(String value) { return value == null ? "" : value.trim(); }
    private void setFlash(HttpSession session, String message, String type) {
        session.setAttribute("scheduleFlash", message); session.setAttribute("scheduleFlashType", type);
    }
}
