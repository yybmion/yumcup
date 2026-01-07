# Stage 1: Build
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Gradle wrapper and dependencies (for caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build application
RUN ./gradlew clean bootJar --no-daemon

# Stage 2: Runtime
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/build/libs/yumcup.jar app.jar

# Set default profile
ENV SPRING_PROFILES_ACTIVE=common

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
