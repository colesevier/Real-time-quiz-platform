<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Game</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/styles.css" />
</head>
<body>
  <div class="container">
    <div id="status">Question 1 / N</div>
    <div id="question">Question text here</div>
    <div id="options">
      <button data-option="A" class="opt">A</button>
      <button data-option="B" class="opt">B</button>
      <button data-option="C" class="opt">C</button>
      <button data-option="D" class="opt">D</button>
    </div>
    <div id="timer">20</div>
    <div id="score">Score: 0</div>
  </div>

  <script src="${pageContext.request.contextPath}/static/js/game.js"></script>
</body>
</html>
