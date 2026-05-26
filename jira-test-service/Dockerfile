FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy parent pom and install to local repo
COPY pom.xml ./pom.xml
RUN apk add --no-cache maven && mvn install -N -DskipTests -q

# Copy and build service
COPY jira-test-service/pom.xml ./pom.xml
COPY jira-test-service/src ./src
RUN mvn package -Dmaven.test.skip=true -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget curl
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8101
ENTRYPOINT ["java", "-jar", "app.jar"]
