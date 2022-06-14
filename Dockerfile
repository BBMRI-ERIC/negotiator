# Build the jar
FROM maven:3-openjdk-17-slim
ARG BRANCH=master
RUN apt update && apt install -y git
RUN git clone https://github.com/BBMRI-ERIC/negotiator-v3.git negotiator
RUN cd negotiator && git checkout ${BRANCH} && mvn clean package -DskipTests

# Build the image
FROM openjdk:17-alpine
COPY --from=0 negotiator/negotiator-application/target/negotiator-application-3.0.1-exec.jar negotiator-application-3.0.1.jar
ENTRYPOINT ["java","-jar", "-Dspring.profiles.active=docker", "/negotiator-application-3.0.1.jar"]