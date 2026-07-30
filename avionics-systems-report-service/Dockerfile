FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Strip modules section to avoid reactor validation
COPY pom.xml ./pom.xml
RUN sed -i '/<modules>/,/<\/modules>/d' pom.xml
RUN mvn install -N -q

# Build this service
COPY avionics-systems-report-service/pom.xml ./pom.xml
COPY avionics-systems-report-service/src ./src
RUN mvn package -Dmaven.test.skip=true -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appuser && adduser -S appuser -G appuser
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser
EXPOSE 8100
ENTRYPOINT ["java","-jar","app.jar"]
