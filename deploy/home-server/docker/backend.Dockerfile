FROM gradle:9.1.0-jdk25 AS build
WORKDIR /home/gradle/project/backend

COPY backend/build.gradle ./build.gradle
COPY backend/src ./src

RUN gradle --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /home/gradle/project/backend/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

