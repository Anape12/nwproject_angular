<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>業務ダッシュボード</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/portal.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-portal
            data-context-path="${pageContext.request.contextPath}"
            data-portal-error="<c:out value='${portalError}' />"
        ></nw-portal>
        <noscript>業務ダッシュボードを表示するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
