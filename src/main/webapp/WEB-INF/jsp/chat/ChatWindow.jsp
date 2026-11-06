<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="utf-8" />
        <title>チャット</title>
        <link rel="stylesheet" href="http://yui.yahooapis.com/3.18.1/build/cssreset/cssreset-min.css" />
        <link href="https://fonts.googleapis.com/css?family=M+PLUS+Rounded+1c" rel="stylesheet" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20260925-1" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/chatLayout.css" />
    </head>
    <body class="chat-page">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-chat-room
            data-context-path="${pageContext.request.contextPath}"
            data-room-id="<c:out value='${requestScope.RoomId}' />"
            data-room-name="<c:out value='${requestScope.RoomName}' />"
            data-user-id="<c:out value='${sessionScope.loginUser.userId}' />"
            data-can-manage="${canManageMembers}"
            data-csrf-token="<c:out value='${sessionScope.attachmentCsrfToken}' />"
            data-messages="<c:out value='${messagesJson}' />"
            data-attachments="<c:out value='${attachmentsJson}' />"
        ></nw-chat-room>
        <noscript>チャットを利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
