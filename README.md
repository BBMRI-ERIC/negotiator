# Negotiator

![build](https://github.com/BBMRI-ERIC/negotiator-v3/actions/workflows/CI.yml/badge.svg?)
![GitHub release](https://img.shields.io/github/v/release/BBMRI-ERIC/negotiator)
[![codecov](https://codecov.io/github/BBMRI-ERIC/negotiator/graph/badge.svg?token=YN9M34IM3S)](https://codecov.io/github/BBMRI-ERIC/negotiator)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/BBMRI-ERIC/negotiator/badge)](https://scorecard.dev/viewer/?uri=github.com/BBMRI-ERIC/negotiator)
![Static Badge](https://img.shields.io/badge/Java%20Code%20Style-Google-orange)
![Static Badge](https://img.shields.io/badge/Docker-bbmrieric%2Fnegotiator-blue)
![License](https://img.shields.io/github/license/BBMRI-ERIC/negotiator)

**Negotiator** is an open-source access negotiation solution tailored for research infrastructures. It offers a highly
customizable workflow engine, along with support for messaging, notifications, and moderation.

## Project Goals

Negotiator aims to streamline access management by providing an extensible and customizable solution for negotiating
access to various resources, including biological sample collections, specialized services, and expertise within
research infrastructures.

## Current Status

The Negotiator is actively used to mediate access to health data and biological samples
within [BBMRI-ERIC](bbmri-eric.eu) and other
European research projects. For more detailed information, refer to our
official [Documentation](https://bbmri-eric.github.io/negotiator).

## Quick Start

### Prerequisites

- Java 17
- Maven
- Spring
- Docker engine

Running the command bellow in your terminal will spin-up the Negotiator in Dev mode
along with a database in a Docker container using [testcontainers](https://testcontainers.com/).
```shell
mvn clean spring-boot:test-run -Dspring-boot.run.profiles=dev 
```

Negotiator application can also be spun up using the provided Docker image.
To run the application with a mock authorization server using the OAuth2 protocol,
see this [docker compose file](.github/oauth-test/compose.yaml).

> [!TIP]
> The Database is available at: ``
jdbc:postgresql://localhost:5432/negotiator
``
> with default credentials negotiator:negotiator

## Development

Pull requests are welcome, please go through the [Pull Request template](.github/pull_request_template.md) before asking
for review.
For contributing, please read our [contribution guidelines](docs/contributing).

## Documentation

Official documentation available at https://bbmri-eric.github.io/negotiator.

## License

Copyright 2020-2024 [BBMRI-ERIC](https://bbmri-eric.eu).

Licensed under GNU Affero General Public License v3.0 (the "License");
you may not use this file except in compliance with the License.

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.
