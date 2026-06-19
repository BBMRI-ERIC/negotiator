# Build jar file with dependencies
FROM maven:3.9.9-eclipse-temurin-21 AS build_image
ARG ARTIFACT_VERSION=unknown
COPY src /app/src
COPY pom.xml /app
WORKDIR /app
RUN mvn --quiet -B versions:set -DnewVersion=$ARTIFACT_VERSION
RUN mvn --quiet -B clean package -Dmaven.test.skip=true


# Runtime image
FROM eclipse-temurin:25-jre-jammy@sha256:c3e62cd0cece58d8de8d760ab95a5014f3b5a6ea32178f54270edb5b4aab9d1f
RUN mkdir /var/log/negotiator && chown 1001 /var/log/negotiator
RUN mkdir -p /app/config/templates && chown 1001 /app/config/templates
USER 1001
WORKDIR /app
COPY --from=build_image /app/target/negotiator-spring-boot.jar /app/negotiator.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s CMD curl -f http://localhost:8081/api/actuator/health || exit 1
ENTRYPOINT ["java","-jar", "negotiator.jar"]
