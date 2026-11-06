<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>設計リバース | NW Project</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/reverse-engineering.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-reverse-engineering data-context-path="${pageContext.request.contextPath}"></nw-reverse-engineering>
        <noscript>設計リバースを利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
