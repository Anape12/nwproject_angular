<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="java.time.LocalDate,java.time.format.DateTimeFormatter,java.util.Locale,java.time.ZoneId" %>
<% String
todayLabel=LocalDate.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("yyyy年M月d日（E）",Locale.JAPANESE));
%>
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
        <main class="workspace-home">
            <a class="mode-switch-link" href="${pageContext.request.contextPath}/MenuSelect" target="_self"
                ><i class="fa-solid fa-table-cells-large"></i> 利用メニューを切り替える</a
            >
            <section class="welcome-panel">
                <div>
                    <p class="home-date"><%=todayLabel%></p>
                    <h1>
                        <c:out value="${sessionScope.loginUser.lastName}" />
                        <c:out value="${sessionScope.loginUser.firstName}" />さん、お疲れさまです。
                    </h1>
                    <p>今日の業務をここから始められます。</p>
                </div>
                <a class="dashboard-link" href="${pageContext.request.contextPath}/Portal"
                    ><span><i class="fa-solid fa-gauge-high"></i></span>
                    <div><strong>業務ダッシュボード</strong><small>予定・通知・未処理をまとめて確認</small></div>
                    <i class="fa-solid fa-arrow-right"></i
                ></a>
            </section>

            <section class="quick-section">
                <div class="section-heading">
                    <div>
                        <span>QUICK START</span>
                        <h2>よく使う操作</h2>
                    </div>
                </div>
                <div class="quick-grid">
                    <a class="quick-card attendance" href="${pageContext.request.contextPath}/WorkManagement"
                        ><span class="card-icon"><i class="fa-solid fa-business-time"></i></span>
                        <div><strong>勤怠を入力</strong><small>打刻・勤務実績・月次申請</small></div>
                        <i class="fa-solid fa-chevron-right"></i
                    ></a>
                    <a class="quick-card report" href="${pageContext.request.contextPath}/DairyWrite"
                        ><span class="card-icon"><i class="fa-solid fa-file-pen"></i></span>
                        <div><strong>報告書を作成</strong><small>下書き・提出状況を確認</small></div>
                        <i class="fa-solid fa-chevron-right"></i
                    ></a>
                    <a class="quick-card schedule" href="${pageContext.request.contextPath}/OpenCalender"
                        ><span class="card-icon"><i class="fa-solid fa-calendar-days"></i></span>
                        <div><strong>予定を確認</strong><small>共有予定・参加依頼を管理</small></div>
                        <i class="fa-solid fa-chevron-right"></i
                    ></a>
                    <a class="quick-card chat" href="${pageContext.request.contextPath}/ChatChanelList"
                        ><span class="card-icon"><i class="fa-solid fa-comments"></i></span>
                        <div><strong>チャットを開く</strong><small>メンバーとすぐにやり取り</small></div>
                        <i class="fa-solid fa-chevron-right"></i
                    ></a>
                </div>
            </section>

            <div class="home-columns">
                <section class="menu-section">
                    <div class="section-heading">
                        <div>
                            <span>COLLABORATION</span>
                            <h2>コミュニケーション</h2>
                        </div>
                    </div>
                    <div class="menu-list">
                        <a href="${pageContext.request.contextPath}/ChatChanelList"
                            ><i class="fa-solid fa-comment-dots"></i>
                            <div><strong>チャット</strong><small>個別・グループチャット</small></div></a
                        >
                        <a href="${pageContext.request.contextPath}/ThreadController"
                            ><i class="fa-solid fa-users"></i>
                            <div><strong>スレッド</strong><small>テーマごとの情報共有</small></div></a
                        >
                        <c:if test="${searchUserInfo}">
                            <a href="${pageContext.request.contextPath}/UserSearch"
                                ><i class="fa-solid fa-address-book"></i>
                                <div><strong>ユーザー検索</strong><small>社員情報を検索・確認</small></div></a
                            >
                        </c:if>
                        <a href="${pageContext.request.contextPath}/QiitaGuide" data-nw-progress-ignore
                            ><i class="fa-solid fa-book-open-reader"></i>
                            <div><strong>Qiita記事を聴く</strong><small>技術記事を検索・解説・読み上げ</small></div></a
                        >
                    </div>
                </section>

                <section class="menu-section">
                    <div class="section-heading">
                        <div>
                            <span>WORKFLOW</span>
                            <h2>申請・管理</h2>
                        </div>
                    </div>
                    <div class="menu-list">
                        <a href="${pageContext.request.contextPath}/Portal"
                            ><i class="fa-solid fa-bell"></i>
                            <div><strong>通知・自分の申請</strong><small>承認結果や取下げを確認</small></div></a
                        >
                        <a href="${pageContext.request.contextPath}/ChangePassword"
                            ><i class="fa-solid fa-key"></i>
                            <div><strong>パスワード変更</strong><small>ログインパスワードを安全に更新</small></div></a
                        >
                        <c:if test="${canReviewApprovals}"
                            ><a href="${pageContext.request.contextPath}/ReportApproval"
                                ><i class="fa-solid fa-circle-check"></i>
                                <div><strong>承認管理</strong><small>未処理申請の確認・一括承認</small></div></a
                            ></c:if
                        >
                        <c:if test="${canManageUsers}"
                            ><a href="${pageContext.request.contextPath}/UserInsert"
                                ><i class="fa-solid fa-user-plus"></i>
                                <div><strong>新規ユーザー登録</strong><small>アカウントと権限を登録</small></div></a
                            ><a href="${pageContext.request.contextPath}/UserSecurityAdmin"
                                ><i class="fa-solid fa-user-shield"></i>
                                <div>
                                    <strong>アカウント管理</strong><small>無効化・ロック解除・パスワード再設定</small>
                                </div></a
                            ></c:if
                        >
                        <c:if test="${canAudit}"
                            ><a href="${pageContext.request.contextPath}/AuditLog"
                                ><i class="fa-solid fa-clipboard-list"></i>
                                <div><strong>監査ログ</strong><small>ログイン・変更・承認操作を検索</small></div></a
                            ></c:if
                        >
                        <c:if test="${canManageAi}"
                            ><a href="${pageContext.request.contextPath}/AiCharacterAdmin"
                                ><i class="fa-solid fa-robot"></i>
                                <div><strong>AI住人管理</strong><small>人格・興味・応答方法を設定</small></div></a
                            ></c:if
                        >
                        <c:if test="${canViewDesign}"
                            ><a href="${pageContext.request.contextPath}/ReverseEngineering"
                                ><i class="fa-solid fa-diagram-project"></i>
                                <div><strong>設計リバース</strong><small>DB・Javaの構造と関係図を確認</small></div></a
                            ></c:if
                        >
                    </div>
                </section>
            </div>

            <c:if test="${canUseExternalTools}"
                ><details class="legacy-tools">
                    <summary><i class="fa-solid fa-toolbox"></i> 外部・管理ツール</summary>
                    <div>
                        <form action="${pageContext.request.contextPath}/SelectApp?AppName=NC30001" method="post">
                            <button><i class="fa-solid fa-cubes"></i> AX3アプリ</button>
                        </form>
                        <form action="${pageContext.request.contextPath}/SelectApp?AppName=NC40001" method="post">
                            <button><i class="fa-solid fa-layer-group"></i> AX4アプリ</button>
                        </form>
                    </div>
                </details></c:if
            >
            <form class="home-logout" action="${pageContext.request.contextPath}/Logout" method="get" target="_self">
                <button><i class="fa-solid fa-right-from-bracket"></i> ログアウト</button>
            </form>
        </main>
        <jsp:include page="/WEB-INF/jsp/login/footer.jsp" />
    </body>
</html>
