FROM openjdk:17-alpine
COPY negotiator-application/target/negotiator-application-3.0.1-exec.jar negotiator-application-3.0.1.jar
ENTRYPOINT ["java","-jar", "-Dspring.profiles.active=docker", "/negotiator-application-3.0.1.jar"]