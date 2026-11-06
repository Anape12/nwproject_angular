<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>AI住人管理</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/ai-character.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-ai-character-admin
            data-context-path="${pageContext.request.contextPath}"
            data-csrf-token="<c:out value='${sessionScope.aiAdminCsrf}' />"
            data-flash="<c:out value='${flash}' />"
            data-flash-type="<c:out value='${flashType}' />"
            data-characters="<c:out value='${charactersJson}' />"
            data-selected="<c:out value='${selectedJson}' />"
            data-jobs="<c:out value='${recentJobsJson}' />"
        ></nw-ai-character-admin>
        <noscript>AI住人管理を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
