FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY gradlew .
RUN chmod +x gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY payment-service/build.gradle payment-service/build.gradle
RUN ./gradlew :payment-service:dependencies --no-daemon

COPY payment-service/src payment-service/src
RUN ./gradlew :payment-service:clean :payment-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/payment-service/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
