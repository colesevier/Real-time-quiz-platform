<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Lobby</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/styles.css" />
</head>
<body>
  <div class="container">
    <h1>Lobby</h1>
    <div id="invite">Invite code: <strong id="code">XXXXXX</strong></div>
    <div id="players">Players (0)</div>
    <div class="actions">
      <button id="start">Start Game</button>
      <button id="cancel">Cancel Game</button>
    </div>
  </div>

  <script src="${pageContext.request.contextPath}/static/js/lobby.js"></script>
</body>
</html>
