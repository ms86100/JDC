FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S jira && adduser -S jira -G jira
COPY target/*.jar app.jar
RUN chown -R jira:jira /app
USER jira
EXPOSE 8084
ENTRYPOINT ["java","-jar","app.jar"]
