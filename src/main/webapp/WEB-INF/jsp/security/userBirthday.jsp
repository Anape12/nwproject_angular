<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>生年月日の確認・変更</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/security.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-user-birthday
            data-context-path="${pageContext.request.contextPath}"
            data-target-user-id="<c:out value='${targetUserId}' />"
            data-csrf-token="<c:out value='${csrfToken}' />"
            data-birthday="<c:out value='${birthday}' />"
        ></nw-user-birthday>
        <noscript>この画面を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
