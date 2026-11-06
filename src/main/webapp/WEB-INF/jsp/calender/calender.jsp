<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>${displayMonth.year}年${displayMonth.monthValue}月 スケジュール</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20260925-1" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/schedule.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-schedule-calendar
            data-context-path="${pageContext.request.contextPath}"
            data-year="${displayMonth.year}"
            data-month="${displayMonth.monthValue}"
            data-previous-year="${previousMonth.year}"
            data-previous-month="${previousMonth.monthValue}"
            data-next-year="${nextMonth.year}"
            data-next-month="${nextMonth.monthValue}"
            data-selected-date="<c:out value='${selectedDate}' />"
            data-csrf-token="<c:out value='${csrfToken}' />"
            data-attachment-csrf="<c:out value='${sessionScope.attachmentCsrfToken}' />"
            data-flash="<c:out value='${flashMessage}' />"
            data-flash-type="<c:out value='${flashType}' />"
            data-event-owner="${eventOwner}"
            data-event-participant="${eventParticipant}"
            data-days="<c:out value='${daysJson}' />"
            data-event="<c:out value='${editEventJson}' />"
            data-users="<c:out value='${scheduleUsersJson}' />"
            data-attachments="<c:out value='${attachmentsJson}' />"
        ></nw-schedule-calendar>
        <noscript>予定を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
