<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>利用メニューを選択｜NW Project</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/menu-mode.css" />
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.6.0/css/all.min.css" />
    </head>
    <body class="mode-body">
        <nw-menu-select
            data-context-path="${pageContext.request.contextPath}"
            data-last-name="<c:out value='${sessionScope.loginUser.lastName}' />"
            data-first-name="<c:out value='${sessionScope.loginUser.firstName}' />"
        ></nw-menu-select>
        <script type="module" src="${pageContext.request.contextPath}/js/angular/main.js"></script>
        <noscript>メニューを利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
