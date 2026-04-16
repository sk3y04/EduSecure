FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace/backend

COPY backend/gradlew ./gradlew
COPY backend/gradle ./gradle
COPY backend/build.gradle ./build.gradle
COPY backend/src ./src

RUN chmod +x ./gradlew \
    && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/backend/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

