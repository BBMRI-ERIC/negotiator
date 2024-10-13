# Negotiator

![build](https://github.com/BBMRI-ERIC/negotiator-v3/actions/workflows/CI.yml/badge.svg?)
[![codecov](https://codecov.io/github/BBMRI-ERIC/negotiator/graph/badge.svg?token=YN9M34IM3S)](https://codecov.io/github/BBMRI-ERIC/negotiator)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/BBMRI-ERIC/negotiator/badge)](https://scorecard.dev/viewer/?uri=github.com/BBMRI-ERIC/negotiator)
![Static Badge](https://img.shields.io/badge/Java%20Code%20Style-Google-orange)
![Static Badge](https://img.shields.io/badge/Docker-bbmrieric%2Fnegotiator-blue)

Negotiator, an open-source access management solution featuring a customizable workflow engine, along with
messaging, notifications and moderation support.

## Goal

This project aims to develop an extensively customizable access management system designed to efficiently structure and
streamline the process of resource access requests within multinational research infrastructures.

## State

Negotiator is currently used for mediating access to biological data and samples in BBMRI-ERIC.
Using one of BBMRI-ERIC
discovery services such as the [Directory](https://directory.bbmri-eric.eu/#/) or
the [GBA SampleLocator](https://samplelocator.bbmri.de/) researchers
can browse and locate collections of bio specimens, and then request access
via [BBMRI-ERIC Negotiator](https://negotiator.bbmri-eric.eu/)
by filling out a request form and then following individual steps of the Negotiation lifecycle.
Reference UI implementation for BBMRI can be found in
this [repository](https://github.com/BBMRI-ERIC/negotiator-v3-frontend).
Documentation for
the new REST API can be found [here](https://negotiator-v3.bbmri-eric.eu/api/swagger-ui/index.html).
An older version of this service can be found in
this [repository](https://github.com/BBMRI-ERIC/negotiator.bbmri).

## The Negotiator as an Access Management System:

### Key Domain entities:

- **Resource**: Any resource/entity that is listed in an external discovery
  service, and has a unique and persistent identifier.
  (e.g., collection of biological samples, research service, specialized treatment...)
- **Request**: A depiction of a query from a data discovery service specifying the resource/resources of
  interest and filtering criteria used to find them in the discovery service.
- **Negotiation**: An access application consisting of one or multiple requests that is linked to an authenticated
  user.
- **Representative**: A physical person responsible for mediating access to a resource in their
  jurisdiction.

### Basic usage example

Using an external discovery service connected to the Negotiator,
the user identifies resources they are interested in getting access to, and passes them as a Request to the Negotiator.
Once authenticated, the user then fills out a resource-specific access form and submits the request for review.
Once the request is approved by an administrator, it becomes a Negotiation where resource representatives,
moderators and the requester can interact with it.

## Quick Start

Negotiator application can also be spun up using the provided Docker image.
To run the application with a mock authorization server using the OAuth2 protocol,
see this [docker compose file](.github/oauth-test/compose.yaml).

To create a request and start the access workflow, run the following curl command:

```shell
curl --location 'http://localhost:8080/api/v3/requests' \
--header 'Content-Type: application/json' \
--data '{
    "url": "https://bbmritestnn.gcc.rug.nl",
    "humanReadable": "#1: No filters used.\r\n#2: No filters used.",
    "resources": [{
        "id": "bbmri-eric:ID:CZ_MMCI:collection:LTS"
    }]
}'
```

## Development

For contributing, please read our [contribution guidelines](docs/CONTRIBUTING.md).

### Prerequisites

- Java 17
- Maven
- Spring
- Docker engine

### Running the backend in development mode
```shell
mvn clean spring-boot:test-run -Dspring-boot.run.profiles=dev 
```

### Connection URL for the Postgres test container database

IN the development mode the application spins up a Docker container with a PostgreSQL relational database.
The Default credentials are: negotiator:negotiator
``
jdbc:postgresql://localhost:5432/negotiator
``

### System architecture

Documentation for individual components:

- [REST API](docs/REST.md)
- [Workflow engine](docs/LIFECYCLE.md)
- [Notification service](docs/NOTIFICATIONS.md)
- [External services interface](docs/EXTERNAL_SERVICES.md)
- [Database migration](docs/DATABASE_MIGRATION.md)
- [Logging policy](docs/LOGGING.md)

## License

Copyright 2020-2024 [BBMRI-ERIC](https://bbmri-eric.eu).

Licensed under GNU Affero General Public License v3.0 (the "License");
you may not use this file except in compliance with the License.

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.
