FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

ARG BUILD_ENV=local
RUN mvn clean package -P${BUILD_ENV} -DskipTests


FROM eclipse-temurin:17-jre

ENV TZ=Asia/Shanghai
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
