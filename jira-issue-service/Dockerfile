FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy all service modules (Maven reactor needs them all due to cross-dependencies)
COPY pom.xml ./pom.xml
COPY jira-gateway ./jira-gateway
COPY jira-auth-service ./jira-auth-service
COPY jira-user-service ./jira-user-service
COPY jira-project-service ./jira-project-service
COPY jira-issue-service ./jira-issue-service
COPY jira-workflow-service ./jira-workflow-service
COPY jira-comment-service ./jira-comment-service
COPY jira-notification-service ./jira-notification-service
COPY jira-search-service ./jira-search-service
COPY jira-audit-service ./jira-audit-service
COPY jira-sprint-service ./jira-sprint-service
COPY jira-attachment-service ./jira-attachment-service
COPY jira-migration-service ./jira-migration-service
COPY jira-plan-service ./jira-plan-service
COPY jira-admin-service ./jira-admin-service
COPY jira-test-service ./jira-test-service
COPY jira-dashboard-service ./jira-dashboard-service
COPY jira-report-service ./jira-report-service
COPY jira-document-service ./jira-document-service
COPY jira-portal-service ./jira-portal-service
COPY jira-component-service ./jira-component-service
COPY jira-version-service ./jira-version-service

RUN apk add --no-cache maven && \
    mvn install -Dmaven.test.skip=true -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget curl
COPY --from=build /app/jira-issue-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]