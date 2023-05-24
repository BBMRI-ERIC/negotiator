![build](https://github.com/BBMRI-ERIC/negotiator-v3/actions/workflows/CI.yml/badge.svg?)
[![codecov](https://codecov.io/github/BBMRI-ERIC/negotiator-v3/graph/badge.svg?token=YN9M34IM3S)](https://codecov.io/github/BBMRI-ERIC/negotiator-v3)
# Negotiator

_An access control service for health data and biological samples._

## Goal

The goal of this project is to provide an access control mechanism by structuring and streamlining negotiation process for resources with restricted access.

## State

Negotiator is currently used for mediating access to biological data and samples in BBMRI-ERIC. Using one of BBMRI-ERIC
discovery services such as the [Directory](https://directory.bbmri-eric.eu/#/) or the [GBA SampleLocator](https://samplelocator.bbmri.de/) researchers
can browse and locate collections of bio specimens, and then request access via [BBMRI-ERIC Negotiator](https://negotiator.bbmri-eric.eu/) by filling out a request form and then following individual steps of the Negotiation lifecycle.
This is a new implementation of the Negotiator, version 3.0.0 and is still under active development.An older version of
this service can be found in this repository: https://github.com/BBMRI-ERIC/negotiator.bbmri

## Quick Start

### Prerequisites
- Docker

The following command will run the Negotiator application with the REST API exposed at: http://localhost:8080
Note: The authentication using OIDC mock server will not work because of issues with docker network, unless the OIDC mock is running
on an external server.
```shell
docker run --rm -e PROFILE=dev -p 8080:8081 negotiator
```

## Development

### Prerequisites
- Java 17
- Maven
- Spring

### Running the backend in dev mode
```shell
mvn package
java -jar -Dspring.profiles.active=dev target/negotiator.jar
```
### Connection URL for the H2 database:
``
jdbc:h2:tcp://localhost:9092/mem:negotiator
``

### Database Design

The database design can be found here: https://dbdiagram.io/d/5f84671a3a78976d7b774fec