FROM eclipse-temurin:21-jre-jammy
RUN addgroup --system quizapp && adduser --system --ingroup quizapp quizapp
WORKDIR /app
COPY target/realtime-quiz-*.jar app.jar
USER quizapp
EXPOSE 8080
ENTRYPOINT ["java","-Xms256m","-Xmx512m","-XX:+UseG1GC","-jar","/app/app.jar"]
