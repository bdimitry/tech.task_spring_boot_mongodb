# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle wrapper + build scripts (кэширование слоёв)
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle gradle.properties ./

RUN chmod +x gradlew

# sources
COPY src ./src

# Собираем fat-jar (Spring Boot)
RUN ./gradlew clean bootJar -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
