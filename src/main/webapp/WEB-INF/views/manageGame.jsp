<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Manage Game</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/styles.css" />
</head>
<body>
  <div class="container">
    <h1>Manage Game</h1>
    <form id="game-meta">
      <label>Title: <input type="text" name="title" required/></label>
      <label>Time per question (s): <input type="number" name="timePerQuestion" value="20" min="5" max="120"/></label>
      <label>Max players: <input type="number" name="maxPlayers" value="50" min="1"/></label>
    </form>

    <section id="questions">
      <h2>Questions</h2>
      <div id="question-list">No questions yet</div>
      <button id="add-question">Add Question</button>
    </section>

    <div class="actions">
      <button id="host-game">Host Game</button>
      <a href="${pageContext.request.contextPath}/dashboard">Cancel</a>
    </div>
  </div>

  <script src="${pageContext.request.contextPath}/static/js/manage.js"></script>
</body>
</html>
