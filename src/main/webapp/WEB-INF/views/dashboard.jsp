<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Dashboard - Quiz</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/styles.css" />
</head>
<body>
  <div class="container">
    <h1>Your Dashboard</h1>
    <div class="controls">
      <a href="${pageContext.request.contextPath}/create" class="btn">Create Game</a>
      <a href="${pageContext.request.contextPath}/join" class="btn">Join Game</a>
      <a href="${pageContext.request.contextPath}/logout" class="btn">Logout</a>
    </div>

    <section id="past-games">
      <h2>Past Games</h2>
      <!-- TODO: render a grid of past games -->
      <div class="grid">No games yet</div>
    </section>
  </div>
</body>
</html>
