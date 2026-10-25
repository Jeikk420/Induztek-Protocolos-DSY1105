FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /project
COPY . .
RUN mvn -B verify
FROM eclipse-temurin:17-jre
ARG SERVICE
ENV SERVICE_NAME=${SERVICE}
WORKDIR /app
COPY --from=build /project/${SERVICE}/target/${SERVICE}-1.0.0.jar /app/service.jar
ENTRYPOINT ["java", "-jar", "/app/service.jar"]
