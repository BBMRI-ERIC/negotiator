# Build jar file with dependencies
FROM maven:3.9.1-eclipse-temurin-17-focal as BUILD_IMAGE
COPY src /app/src
COPY pom.xml /app
WORKDIR /app
RUN mvn --quiet -B clean package -Dmaven.test.skip=true


# Runtime image
FROM eclipse-temurin:17-jre-focal
USER 1001
WORKDIR /app
COPY --from=BUILD_IMAGE /app/target/negotiator-spring-boot.jar /app/negotiator.jar
EXPOSE 8081
RUN mkdir /var/log/negotiator
HEALTHCHECK --interval=30s --timeout=10s CMD curl -f http://localhost:8081/api/actuator/health || exit 1
ENTRYPOINT ["java","-jar", "-Dspring.profiles.active=${PROFILE}", "negotiator.jar"]
