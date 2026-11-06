<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
<meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
<title>個別チャット作成</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style55.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-chat-create
            data-context-path="${pageContext.request.contextPath}"
            data-rows="<c:out value='${chatUsersJson}' />"
            data-error-message="<c:out value='${errorMessage}' />"
        ></nw-chat-create>
        <noscript>この画面を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
