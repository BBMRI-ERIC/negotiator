![build](https://github.com/BBMRI-ERIC/negotiator-v3/actions/workflows/CI.yml/badge.svg?)
[![codecov](https://codecov.io/github/BBMRI-ERIC/negotiator-v3/graph/badge.svg?token=YN9M34IM3S)](https://codecov.io/github/BBMRI-ERIC/negotiator-v3)
# Negotiator

_An access control service for health data and biological samples._

## Goal

The goal of this project is to provide an access control mechanism by structuring and streamlining negotiation process.

## State

Negotiator is currently used for mediating access to biological data and samples in BBMRI-ERIC. Using one of BBMRI-ERIC
discovery services such as the [Directory](https://directory.bbmri-eric.eu/#/) or the [GBA SampleLocator](https://samplelocator.bbmri.de/) researchers
can browse and locate collections of bio specimens, and then request access via [BBMRI-ERIC Negotiator](https://negotiator.bbmri-eric.eu/).

## Version

This is a new implementation of the Negotiator, version 3.0.0 and is still under active development.An older version of
this service can be found in this repository: https://github.com/BBMRI-ERIC/negotiator.bbmri

## Quick Start

TODO

## Development mode

See [Development](docs/development.md)

### Database Design

The database design can be found here: https://dbdiagram.io/d/5f84671a3a78976d7b774fec

## Installation

### Using docker

To create a test version of the negotiator perform the following steps:

1. Clone this repository using `git clone https://github.com/BBMRI-ERIC/negotiator-v3.git`
2. cd into the directory: `cd negotiator-v3`
3. Run the docker image build `docker-compose build negotiator`
4. Run the compose `docker-compose up -d`

The negotiator will run using Postgres as DBMS, and it will respond on `http://localhost:8080`.

Some [test data](/src/main/resources/data-h2.sql) will also be added 

## References
