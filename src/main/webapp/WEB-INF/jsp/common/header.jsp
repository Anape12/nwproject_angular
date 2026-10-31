<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="jp.nw.entity.UserEntity" %>

<% UserEntity loginUser = (UserEntity)session.getAttribute("loginUser"); %>

<div class="header">
    <a class="logo" href="<%=request.getContextPath()%>/MenuSelect">NW Project</a>
    <nav class="header-nav">
        <a href="<%=request.getContextPath()%>/BusinessMenu">業務メニュー</a>
        <a href="<%=request.getContextPath()%>/MenuSelect">切替</a>
        <a href="<%=request.getContextPath()%>/WorkManagement">勤怠</a>
        <a href="<%=request.getContextPath()%>/OpenCalender">予定</a>
        <a href="<%=request.getContextPath()%>/ChatChanelList">チャット</a>
        <a href="<%=request.getContextPath()%>/UserBirthday">生年月日</a>
    </nav>
    <div class="login-user">
        <% if(loginUser != null){ %> <%=loginUser.getLastName()%> <%=loginUser.getFirstName()%> さん ログイン中 <% } %>
    </div>
</div>
