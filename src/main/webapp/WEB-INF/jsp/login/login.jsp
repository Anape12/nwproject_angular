<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style11.css?v=20261010-1" />
        <title>${loginFailed ? 'ログインできませんでした | NW Project' : 'ログイン | NW Project'}</title>
    </head>
    <body class="${loginFailed ? 'auth-result-page' : ''}">
        <nw-login
            data-login-url="${pageContext.request.contextPath}/Login"
            data-failed="${loginFailed}"
            data-error-message="<c:out value='${errorMessage}' />"
        ></nw-login>
        <noscript>ログインするには JavaScript を有効にしてください。</noscript>
        <script type="module" src="${pageContext.request.contextPath}/js/angular/main.js"></script>
    </body>
</html>
