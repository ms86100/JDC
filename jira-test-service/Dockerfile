FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Strip modules section to avoid reactor validation
COPY pom.xml ./pom.xml
RUN sed -i '/<modules>/,/<\/modules>/d' pom.xml
RUN mvn install -N -q

# Build this service
COPY jira-test-service/pom.xml ./pom.xml
COPY jira-test-service/src ./src
RUN mvn package -Dmaven.test.skip=true -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S jira && adduser -S jira -G jira
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R jira:jira /app
USER jira
EXPOSE 8101
ENTRYPOINT ["java","-jar","app.jar"]
