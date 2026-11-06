<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>メインメニュー（<c:out value="${sessionScope.resultApp}" />）</title>
    </head>
    <body style="background: #63515f">
        <nw-external-app
            data-context-path="${pageContext.request.contextPath}"
            data-app-name="<c:out value='${sessionScope.resultApp}' />"
            data-read-mode="true"
        ></nw-external-app>
        <noscript>この画面を利用するには JavaScript を有効にしてください。</noscript>
        <script type="module" src="${pageContext.request.contextPath}/js/angular/main.js"></script>
    </body>
</html>
