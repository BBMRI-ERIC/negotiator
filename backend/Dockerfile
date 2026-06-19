# Build jar file with dependencies
FROM maven:3.9.16-eclipse-temurin-21-noble@sha256:d7e7f57407437c014571f1ad5a9955f03fc3edcb1d964067ef351fa38e798665 AS build_image
ARG ARTIFACT_VERSION=unknown
COPY src /app/src
COPY pom.xml /app
WORKDIR /app
RUN mvn --quiet -B versions:set -DnewVersion=$ARTIFACT_VERSION
RUN mvn --quiet -B clean package -Dmaven.test.skip=true


# Runtime image
FROM eclipse-temurin:21-jre-noble@sha256:1038345f7b7211c290148801b92d66abe5588d4acd6820994df43b883cdfc04c
RUN mkdir /var/log/negotiator && chown 1001 /var/log/negotiator
RUN mkdir -p /app/config/templates && chown 1001 /app/config/templates
USER 1001
WORKDIR /app
COPY --from=build_image /app/target/negotiator-spring-boot.jar /app/negotiator.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s CMD curl -f http://localhost:8081/api/actuator/health || exit 1
ENTRYPOINT ["java","-jar", "negotiator.jar"]
