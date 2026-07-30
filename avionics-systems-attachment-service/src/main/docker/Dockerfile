FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache shadow && \
    useradd -r -m appuser
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /var/avionics-systems/attachments && chown -R appuser:appuser /var/avionics-systems
USER appuser
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
