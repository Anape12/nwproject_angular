<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>スレッド詳細</title>
        <link href="https://fonts.googleapis.com/css?family=M+PLUS+Rounded+1c" rel="stylesheet" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style33.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-thread-detail
            data-context-path="${pageContext.request.contextPath}"
            data-thread="<c:out value='${threadJson}' />"
            data-csrf-token="<c:out value='${sessionScope.threadCsrfToken}' />"
            data-flash="<c:out value='${threadFlash}' />"
            data-flash-type="<c:out value='${threadFlashType}' />"
            data-pending="${aiResponsePending}"
        ></nw-thread-detail>
        <noscript>スレッドを表示するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
