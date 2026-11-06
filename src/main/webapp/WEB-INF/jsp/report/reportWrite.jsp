<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>報告書作成</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20260925-1" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/report.css?v=20260925-1" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-report-write
            data-context-path="${pageContext.request.contextPath}"
            data-csrf-token="<c:out value='${csrfToken}' />"
            data-attachment-csrf="<c:out value='${sessionScope.attachmentCsrfToken}' />"
            data-today="<c:out value='${today}' />"
            data-flash="<c:out value='${flashMessage}' />"
            data-flash-type="<c:out value='${flashType}' />"
            data-reports="<c:out value='${reportsJson}' />"
            data-selected="<c:out value='${selectedReportJson}' />"
            data-attachments="<c:out value='${attachmentsJson}' />"
        ></nw-report-write>
        <noscript>報告書を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
