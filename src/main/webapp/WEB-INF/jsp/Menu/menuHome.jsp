<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <base target="_blank" />
        <title>ホーム｜NW Project</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style11.css" />
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.6.0/css/all.min.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-business-menu
            data-context-path="${pageContext.request.contextPath}"
            data-today-label="<c:out value='${todayLabel}' />"
            data-last-name="<c:out value='${sessionScope.loginUser.lastName}' />"
            data-first-name="<c:out value='${sessionScope.loginUser.firstName}' />"
            data-search-user-info="<c:out value='${searchUserInfo}' />"
            data-can-review-approvals="<c:out value='${canReviewApprovals}' />"
            data-can-manage-users="<c:out value='${canManageUsers}' />"
            data-can-audit="<c:out value='${canAudit}' />"
            data-can-manage-ai="<c:out value='${canManageAi}' />"
            data-can-view-design="<c:out value='${canViewDesign}' />"
            data-can-use-external-tools="<c:out value='${canUseExternalTools}' />"
        ></nw-business-menu>
        <noscript>この画面を利用するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
