<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>承認管理</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20260925-1" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/report.css?v=20260925-1" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-report-approval
            data-context-path="${pageContext.request.contextPath}"
            data-csrf-token="<c:out value='${csrfToken}' />"
            data-flash="<c:out value='${flashMessage}' />"
            data-flash-type="<c:out value='${flashType}' />"
            data-can-configure="${canConfigureApprovals}"
            data-applications="<c:out value='${applicationsJson}' />"
            data-selected="<c:out value='${selectedJson}' />"
        ></nw-report-approval>
        <noscript>承認管理を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
