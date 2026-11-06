<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>チャットメンバー管理</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style55.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-chat-members
            data-context-path="${pageContext.request.contextPath}"
            data-room-id="<c:out value='${roomId}' />"
            data-csrf-token="<c:out value='${sessionScope.chatMemberCsrf}' />"
            data-flash="<c:out value='${flash}' />"
            data-members="<c:out value='${membersJson}' />"
            data-available="<c:out value='${availableJson}' />"
        ></nw-chat-members>
        <noscript>メンバー管理を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
