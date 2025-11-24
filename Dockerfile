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
# --no-daemon: Ensures Gradle shuts down after building (saves memory/stuck processes)
RUN chmod +x gradlew && ./gradlew build --no-daemon && rm build/libs/*-plain.jar

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR file from the build stage to the container
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]