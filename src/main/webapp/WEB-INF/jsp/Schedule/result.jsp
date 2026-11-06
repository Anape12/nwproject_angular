<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>予定の登録結果</title>
    </head>
    <body style="background:#deafd3;">
        <nw-result
            data-kind="calendar"
            data-context-path="${pageContext.request.contextPath}"
        ></nw-result>
        <script type="module" src="${pageContext.request.contextPath}/js/angular/main.js"></script>
        <noscript>結果を表示するには JavaScript を有効にしてください。</noscript>
    </body>
</html>
