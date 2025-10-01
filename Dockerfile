# Simple runtime-only Dockerfile
# Build the JAR first with: ./gradlew clean bootJar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the pre-built JAR
COPY build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
