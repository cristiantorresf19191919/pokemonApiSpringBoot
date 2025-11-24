# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS build

# Set the working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./

# Copy source code
COPY src/ src/

# Make gradlew executable and build the application
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage to the container
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port your application runs on
EXPOSE 8080

# Define the command to run your application
ENTRYPOINT ["java", "-jar", "app.jar"]

