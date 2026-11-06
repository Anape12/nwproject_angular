<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>パスワード変更</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/security.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-change-password
            data-context-path="${pageContext.request.contextPath}"
            data-error-message="<c:out value='${errorMessage}' />"
            data-csrf-token="<c:out value='${csrfToken}' />"
        ></nw-change-password>
        <noscript>この画面を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
