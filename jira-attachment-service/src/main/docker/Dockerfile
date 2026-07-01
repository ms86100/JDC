FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache shadow && \
    useradd -r -m jira
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /var/jira/attachments && chown -R jira:jira /var/jira
USER jira
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
