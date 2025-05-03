# Negotiator

![build](https://github.com/BBMRI-ERIC/negotiator-v3/actions/workflows/CI.yml/badge.svg?)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=BBMRI-ERIC_negotiator&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=BBMRI-ERIC_negotiator)
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

- Docker Engine 27.0 and newer
- Docker Compose 2.30 and newer
- Host networking enabled (See this [link](https://docs.docker.com/engine/network/drivers/host/) for Docker Desktop)
- Unallocated Ports 8080, 8081 and 5432

To launch a local instance of the Negotiator, run the following commands:
```shell
git clone https://github.com/BBMRI-ERIC/negotiator.git
cd negotiator
docker compose up -d
```


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

Copyright 2020-2025 [BBMRI-ERIC](https://bbmri-eric.eu).

Licensed under GNU Affero General Public License v3.0 (the "License");
you may not use this file except in compliance with the License.

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.

