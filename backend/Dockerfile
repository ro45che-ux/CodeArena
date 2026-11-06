# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
# A JDK (not a JRE) is required because the judge compiles and runs user submissions with javac/java.
# Pinned to Java 21: the judge sandbox relies on the Security Manager, which was removed in Java 24+.
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Run as an unprivileged user so submitted code (even if it escaped the in-JVM sandbox) cannot
# modify the application, and has no rights beyond this user's.
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin judge
COPY --from=build --chown=root:root /app/target/*.jar app.jar
USER judge

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]
