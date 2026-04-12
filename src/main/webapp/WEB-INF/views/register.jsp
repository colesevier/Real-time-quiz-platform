<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Register - Quiz</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/styles.css" />
</head>
<body>
  <div class="container">
    <h1>Register</h1>
    <% if (request.getAttribute("error") != null) { %>
      <div class="error"><%= request.getAttribute("error") %></div>
    <% } %>
    <form method="post" action="${pageContext.request.contextPath}/register">
      <label>Username: <input type="text" name="username" required></label><br />
      <label>Email: <input type="email" name="email" required></label><br />
      <label>Password: <input type="password" name="password" required></label><br />
      <label>Confirm Password: <input type="password" name="confirm" required></label><br />
      <button type="submit">Register</button>
    </form>
    <p><a href="${pageContext.request.contextPath}/login">Back to login</a></p>
  </div>
</body>
</html>
