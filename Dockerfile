# Build jar file with dependencies
FROM maven:3.9.1-eclipse-temurin-17-focal@9a2120bf709b8ed0eef46e13bbdf6ab63fb18b529710c275b68190457728f246 as BUILD_IMAGE
ARG ARTIFACT_VERSION=unknown
COPY src /app/src
COPY pom.xml /app
WORKDIR /app
RUN mvn --quiet -B versions:set -DnewVersion=$ARTIFACT_VERSION
RUN mvn --quiet -B clean package -Dmaven.test.skip=true


# Runtime image
FROM eclipse-temurin:17-jre-focal@9a2120bf709b8ed0eef46e13bbdf6ab63fb18b529710c275b68190457728f246
RUN mkdir /var/log/negotiator && chown 1001 /var/log/negotiator
USER 1001
WORKDIR /app
COPY --from=BUILD_IMAGE /app/target/negotiator-spring-boot.jar /app/negotiator.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s CMD curl -f http://localhost:8081/api/actuator/health || exit 1
ENTRYPOINT ["java","-jar", "negotiator.jar"]
