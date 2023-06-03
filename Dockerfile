FROM gradle:8.6.0-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM openjdk:17-jdk-alpine

RUN mkdir /app
COPY --from=build /home/gradle/src/core/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]