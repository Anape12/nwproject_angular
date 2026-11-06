<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style55.css" />
        <title>ユーザー検索</title>
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-user-search
            data-context-path="${pageContext.request.contextPath}"
            data-user-id="<c:out value='${searchUserId}' />"
            data-search-failed="${searchFailed}"
            data-results="<c:out value='${searchResultsJson}' />"
        ></nw-user-search>
        <noscript>ユーザー検索を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
