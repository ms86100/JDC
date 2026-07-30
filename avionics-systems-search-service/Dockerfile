FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appuser && adduser -S appuser -G appuser
RUN mkdir -p /var/avionics-systems && chown -R appuser:appuser /var/avionics-systems
COPY target/*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser
EXPOSE 8088
ENTRYPOINT ["java","-jar","app.jar"]
