FROM amazoncorretto:21-alpine-jdk

ARG JAR_FILE=build/libs/yumcup.jar
ARG PROFILES
ARG ENV

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=${PROFILES}", "-Dserver.env=${ENV}", "-Dspring.config.location=/config/application-secret.yml", "-jar", "app.jar"]