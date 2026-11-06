<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<nw-header
    data-context-path="${pageContext.request.contextPath}"
    data-last-name="<c:out value='${sessionScope.loginUser.lastName}' />"
    data-first-name="<c:out value='${sessionScope.loginUser.firstName}' />"
    data-logged-in="${not empty sessionScope.loginUser}"
></nw-header>
<script type="module" src="${pageContext.request.contextPath}/js/angular/main.js"></script>
