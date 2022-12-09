# Build the image
FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get upgrade -y && \
    apt-get purge curl wget libbinutils libctf0 libctf-nobfd0 libncurses6 -y && \
    apt-get autoremove -y && apt-get clean

RUN mkdir -p /app && chown -R 1001:1001 /app

COPY target/negotiator-exec.jar /app/negotiator.jar
WORKDIR /app
USER 1001
ENTRYPOINT ["java","-jar", "-Dspring.profiles.active=prod", "/app/negotiator.jar"]