# Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
RUN mkdir -p /app/data && chown -R 1000:1000 /app/data
VOLUME /app/data
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests


# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/self-search*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
