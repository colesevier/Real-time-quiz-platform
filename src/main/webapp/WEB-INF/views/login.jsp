<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Login - Quiz</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/styles.css" />
</head>
<body>
  <div class="container">
    <h1>Login</h1>
    <% if (request.getAttribute("error") != null) { %>
      <div class="error"><%= request.getAttribute("error") %></div>
    <% } %>
    <form method="post" action="${pageContext.request.contextPath}/login">
      <label>Username: <input type="text" name="username" required></label><br />
      <label>Password: <input type="password" name="password" required></label><br />
      <button type="submit">Login</button>
    </form>
    <p><a href="${pageContext.request.contextPath}/register">Register</a> | <a href="#" onclick="guest()">Login as Guest</a></p>
  </div>

  <script>
    function guest(){
      let name = prompt('Enter a guest name');
      if(name){
        // store guest name in sessionStorage and redirect to join
        sessionStorage.setItem('guestName', name);
        window.location.href = '${pageContext.request.contextPath}/dashboard';
      }
    }
  </script>
</body>
</html>
