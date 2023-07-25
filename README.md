# Negotiator

![build](https://github.com/BBMRI-ERIC/negotiator-v3/actions/workflows/CI.yml/badge.svg?)
[![UnitTest Coverage](https://codecov.io/github/BBMRI-ERIC/negotiator-v3/graph/badge.svg?token=YN9M34IM3S)](https://codecov.io/github/BBMRI-ERIC/negotiator-v3)
![Static Badge](https://img.shields.io/badge/Java%20Code%20Style-Google-orange)
![Static Badge](https://img.shields.io/badge/Docker-bbmrieric%2Fnegotiator-blue)

Negotiator, an open-source system for streamlining access request workflows in multinational environments.
Allows defining a custom workflow engine, provides a REST API for interaction with requests and features
for moderation on national level.

<!-- TOC -->
* [Negotiator](#negotiator)
  * [Goal](#goal)
  * [State](#state)
  * [Quick Start](#quick-start)
  * [Development](#development)
    * [Prerequisites](#prerequisites)
    * [Running the backend in dev mode](#running-the-backend-in-dev-mode)
    * [Connection URL for the H2 database](#connection-url-for-the-h2-database)
    * [Architecture](#architecture)
<!-- TOC -->

## Goal

The goal of this project is to provide a highly customizable system, featuring an access control mechanism
for structuring and streamlining the process of access requests for resources under the jurisdiction of different
organizations spanning multiple nations and, each with their own legislation.

## State

Negotiator is currently used for mediating access to biological data and samples in BBMRI-ERIC. Using one of BBMRI-ERIC
discovery services such as the [Directory](https://directory.bbmri-eric.eu/#/) or
the [GBA SampleLocator](https://samplelocator.bbmri.de/) researchers
can browse and locate collections of bio specimens, and then request access
via [BBMRI-ERIC Negotiator](https://negotiator.bbmri-eric.eu/)
by filling out a request form and then following individual steps of the Negotiation lifecycle.
This is a new implementation of the Negotiator, version 3.0.0 and is still under active development.
Reference UI implementation for BBMRI can be found in this [repository](https://github.com/BBMRI-ERIC/negotiator-v3-frontend).
Documentation for
the new REST API can be found [here](https://negotiator-v3.bbmri-eric.eu/api/swagger-ui/index.html).
An older version of this service can be found in this [repository](https://github.com/BBMRI-ERIC/negotiator.bbmri).
## Key concepts of the domain
Key concepts for understanding the terminology:
- **Resource**: Any resource/entity that is made available and discoverable in a data discovery service that has a unique and persistent identifier
(e.g., collection of biological samples, research service, specialized treatment...)
- **Request**: A request originating from a data discovery service specifying the resource of interest and the filtering criteria.
- **Negotiation**: An access application consisting of multiple requests linked to an authenticated user
- **Representative**: A physical person responsible for mediating access to a resource in their jurisdiction
## Quick Start

The following command will run the Negotiator application with the REST API exposed
at [port 8080](http://localhost:8080)
Note: The authentication using OIDC mock server will not work because of issues with docker network, unless the OIDC
mock is running
on an external server.

```shell
docker run --rm -e PROFILE=dev -p 8080:8081 bbmrieric/negotiator:latest
```

## Development

### Prerequisites

- Java 17
- Maven
- Spring

### Running the backend in dev mode

```shell
mvn clean package
java -jar -Dspring.profiles.active=dev target/negotiator.jar
```

### Connection URL for the H2 database

``
jdbc:h2:tcp://localhost:9092/mem:negotiator
``
### Architecture
Negotiator follows a classic repository-service pattern. Key components:
- [REST API](docs/REST.md)
- [Workflow engine](docs/LIFECYCLE.md)
- Notification service (UPCOMING)
- Sync service (UPCOMING)