<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>生年月日の確認・変更</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/security.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <main class="security-page">
            <h1>生年月日の確認・変更</h1>
            <p>対象ユーザー：<c:out value="${targetUserId}" /></p>
            <form method="post" action="${pageContext.request.contextPath}/UserBirthday">
                <input type="hidden" name="csrfToken" value="${fn:escapeXml(csrfToken)}" />
                <input type="hidden" name="userId" value="${fn:escapeXml(targetUserId)}" />
                <label>生年月日 <input type="date" name="birthday" value="${fn:escapeXml(birthday)}" required /></label>
                <button type="submit">保存する</button>
            </form>
        </main>
    </body>
</html>
