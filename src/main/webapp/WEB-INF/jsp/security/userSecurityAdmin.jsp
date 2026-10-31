<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>アカウント・セキュリティ管理</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/security.css" />
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />

        <main class="security-page">
            <div class="page-head">
                <div>
                    <p>ADMINISTRATION</p>
                    <h1>アカウント・セキュリティ管理</h1>
                </div>
                <a href="${pageContext.request.contextPath}/AuditLog">監査ログを検索</a>
            </div>

            <c:if test="${not empty flash}">
                <div class="alert ${flashType}">
                    <c:out value="${flash}" />
                </div>
            </c:if>

            <div class="user-security-grid">
                <c:forEach var="u" items="${users}">
                    <section class="security-card user-card">
                        <div class="user-title">
                            <div>
                                <strong><c:out value="${u.lastName} ${u.firstName}" /></strong>
                                <small> <c:out value="${u.userId}" />・${fn:escapeXml(u.accountType)} </small>
                            </div>
                            <span class="status ${u.accountDisabled ? 'disabled' : 'active'}">
                                ${u.accountDisabled ? '無効' : '有効'}
                            </span>
                        </div>

                        <dl>
                            <dt>最終ログイン</dt>
                            <dd><c:out value="${empty u.lastLoginAt ? '-' : u.lastLoginAt}" /></dd>
                            <dt>ロック</dt>
                            <dd><c:out value="${empty u.lockedUntil ? '-' : u.lockedUntil}" /></dd>
                            <dt>失敗回数</dt>
                            <dd><c:out value="${u.failedLoginCount}" /></dd>
                            <dt>初回変更</dt>
                            <dd>${u.forcePasswordChange ? '必要' : '不要'}</dd>
                        </dl>

                        <c:if test="${u.accountType != 'AI' and (u.permission != '0' or canManageSystemUsers)}">
                            <c:url var="birthdayUrl" value="/UserBirthday"
                                ><c:param name="userId" value="${u.userId}"
                            /></c:url>
                            <a href="${fn:escapeXml(birthdayUrl)}">生年月日を確認・変更</a>
                            <form method="post">
                                <input type="hidden" name="csrfToken" value="${csrfToken}" />
                                <input type="hidden" name="targetUserId" value="${u.userId}" />
                                <input type="hidden" name="action" value="updateProfile" />

                                <div class="form-grid">
                                    <label>
                                        姓
                                        <input name="lastName" value="${u.lastName}" required />
                                    </label>
                                    <label>
                                        名
                                        <input name="firstName" value="${u.firstName}" required />
                                    </label>
                                    <label>
                                        権限
                                        <select name="permission">
                                            <c:choose>
                                                <c:when test="${u.permission == '0'}">
                                                    <option value="0" selected>システム権限者</option>
                                                    <option value="1">管理者</option>
                                                    <option value="2">一般ユーザー</option>
                                                </c:when>
                                                <c:when test="${u.permission == '1'}">
                                                    <c:if test="${canManageSystemUsers}"
                                                        ><option value="0">システム権限者</option></c:if
                                                    >
                                                    <option value="1" selected>管理者</option>
                                                    <option value="2">一般ユーザー</option>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:if test="${canManageSystemUsers}"
                                                        ><option value="0">システム権限者</option></c:if
                                                    >
                                                    <option value="1">管理者</option>
                                                    <option value="2" selected>一般ユーザー</option>
                                                </c:otherwise>
                                            </c:choose>
                                        </select>
                                    </label>
                                </div>

                                <button type="submit">基本情報・権限を更新</button>
                            </form>

                            <div class="action-row">
                                <form method="post">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}" />
                                    <input type="hidden" name="targetUserId" value="${u.userId}" />
                                    <input
                                        type="hidden"
                                        name="action"
                                        value="${u.accountDisabled ? 'enable' : 'disable'}"
                                    />
                                    <button class="${u.accountDisabled ? 'primary' : 'danger'}" type="submit">
                                        ${u.accountDisabled ? '有効化' : '無効化'}
                                    </button>
                                </form>

                                <form method="post">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}" />
                                    <input type="hidden" name="targetUserId" value="${u.userId}" />
                                    <input type="hidden" name="action" value="unlock" />
                                    <button type="submit">ロック解除</button>
                                </form>
                            </div>

                            <form method="post" class="reset-form">
                                <input type="hidden" name="csrfToken" value="${csrfToken}" />
                                <input type="hidden" name="targetUserId" value="${u.userId}" />
                                <input type="hidden" name="action" value="resetPassword" />
                                <label>
                                    仮パスワード
                                    <input
                                        type="password"
                                        name="temporaryPassword"
                                        minlength="8"
                                        required
                                        autocomplete="new-password"
                                    />
                                </label>
                                <button type="submit">パスワード再設定</button>
                            </form>
                        </c:if>
                    </section>
                </c:forEach>
            </div>
        </main>
    </body>
</html>
