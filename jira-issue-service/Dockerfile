FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Step 1: Strip modules section from root pom and install as parent
COPY pom.xml ./pom.xml
RUN sed -i '/<modules>/,/<\/modules>/d' pom.xml
RUN mvn install -N -q

# Step 2: Copy and build dependency services to local .m2
COPY jira-project-service ./jira-project-service
RUN cd jira-project-service && mvn install -Dmaven.test.skip=true -q
COPY jira-admin-service ./jira-admin-service
RUN cd jira-admin-service && mvn install -Dmaven.test.skip=true -q

# Step 3: Build issue-service (skip BOTH test compilation and execution)
COPY jira-issue-service/pom.xml ./pom.xml
COPY jira-issue-service/src ./src
RUN mvn package -Dmaven.test.skip=true -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S jira && adduser -S jira -G jira
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R jira:jira /app
USER jira
EXPOSE 8084
ENTRYPOINT ["java","-jar","app.jar"]
