# Prerequisites
- Java 19
- Maven
- Spring

# Running the backend in dev mode
```shell
mvn package
java -jar -Dspring.profiles.active=dev target/negotiator.jar
```