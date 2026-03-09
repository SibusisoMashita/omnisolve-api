FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy pre-built JAR from CI pipeline
COPY target/omnisolve-api-0.0.1.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

