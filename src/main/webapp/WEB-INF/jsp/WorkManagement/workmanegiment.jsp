<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>${displayMonth.year}年${displayMonth.monthValue}月 勤怠表</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance-approval.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-attendance
            data-context-path="${pageContext.request.contextPath}"
            data-year="${displayMonth.year}"
            data-month="${displayMonth.monthValue}"
            data-month-key="${displayMonth}"
            data-previous-year="${previousMonth.year}"
            data-previous-month="${previousMonth.monthValue}"
            data-next-year="${nextMonth.year}"
            data-next-month="${nextMonth.monthValue}"
            data-closed="${monthClosed}"
            data-can-close="${canCloseAttendance}"
            data-selected-date="<c:out value='${selectedDate}' />"
            data-csrf-token="<c:out value='${csrfToken}' />"
            data-flash="<c:out value='${flashMessage}' />"
            data-flash-type="<c:out value='${flashType}' />"
            data-days="<c:out value='${attendanceDaysJson}' />"
            data-summary="<c:out value='${attendanceSummaryJson}' />"
            data-selected="<c:out value='${selectedJson}' />"
            data-reports="<c:out value='${reportsJson}' />"
        ></nw-attendance>
        <noscript>勤怠表を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
