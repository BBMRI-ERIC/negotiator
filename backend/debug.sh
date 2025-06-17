#!/bin/bash
# This script is used to perform remote debugging a Spring Boot application in debug mode in WSL or Linux environment
mvn clean spring-boot:test-run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"