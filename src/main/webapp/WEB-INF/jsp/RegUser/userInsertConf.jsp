<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>登録情報確認</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    </head>
    <body style="background:#9ad468;">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <nw-result
            data-kind="registration"
            data-context-path="${pageContext.request.contextPath}"
        ></nw-result>
        <noscript>結果を表示するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
