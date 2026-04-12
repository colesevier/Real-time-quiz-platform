# Real-Time Quiz Platform

This repository contains a Java-based real-time quiz platform skeleton (Servlets + JSP + WebSocket) intended to be deployed to Apache Tomcat 10 and backed by MySQL 8.

Overview
- Java 21
- Jakarta Servlets / WebSocket
- MySQL 8 (database name: kahoot)
- jBCrypt for password hashing
- JUnit 5 for unit tests

Getting started
1. Create a MySQL database called `kahoot` and run the SQL in `sql/schema.sql`.
2. Configure DB connection using environment variables:
   - QUIZ_DB_URL (e.g., jdbc:mysql://localhost:3306/kahoot?useSSL=false&serverTimezone=UTC)
   - QUIZ_DB_USER
   - QUIZ_DB_PASS
3. Build the WAR:

```bash
mvn clean package
```

4. Deploy the generated `target/realtime-quiz.war` to Apache Tomcat 10 (drop in the `webapps/` folder or use the manager)

Notes & next steps
- The project contains skeleton servlets, DAOs, JSP views, and a simple WebSocket endpoint. It is intended as a starting point to implement the full feature set in the specification.
- Consider switching to Spring Boot (with STOMP over WebSocket) if you want richer message routing and easier development for STOMP topics.
