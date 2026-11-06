<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>新規ユーザー登録</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-registration.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-user-registration
            data-context-path="${pageContext.request.contextPath}"
            data-csrf-token="<c:out value='${csrfToken}' />"
            data-permissions="<c:out value='${permissionLevelsJson}' />"
            data-success="<c:out value='${successMessage}' />"
            data-error="<c:out value='${errorMessage}' />"
            data-last-name="<c:out value='${enteredLastName}' />"
            data-first-name="<c:out value='${enteredFirstName}' />"
            data-birthday="<c:out value='${enteredBirthday}' />"
            data-user-id="<c:out value='${enteredUserId}' />"
            data-permission="<c:out value='${enteredPermission}' />"
            data-current-date="<c:out value='${currentDate}' />"
            data-expiration="<c:out value='${not empty enteredExpiration ? enteredExpiration : defaultExpiration}' />"
        ></nw-user-registration>
        <noscript>ユーザー登録を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
