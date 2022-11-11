![build](https://github.com/BBMRI-ERIC/negotiator-v3/actions/workflows/CI.yml/badge.svg?)
[![codecov](https://codecov.io/github/BBMRI-ERIC/negotiator-v3/graph/badge.svg?token=YN9M34IM3S)](https://codecov.io/github/BBMRI-ERIC/negotiator-v3)
# BBMRI-ERIC Negotiator

The BBMRI-ERIC Negotiator service for structured temp with biological resources dealing with
human data and/or biological samples.

## Version

This is a new implementation of the Negotiator. Stating with version 3.0.0. Older version of this
service can be found on this repository: https://github.com/BBMRI-ERIC/negotiator.bbmri

## Getting Started

A development version of the service can be found here:

## Development mode

### Database Design

The database design can be found here: https://dbdiagram.io/d/5f84671a3a78976d7b774fec

## Installation

### Using docker

To create a test version of the negotiator perform the following steps:

1. Clone this repository using `git clone https://github.com/BBMRI-ERIC/negotiator-v3.git`
2. cd into the directory: `cd negotiator-v3`
3. Run the docker image build `docker-compose build negotiator`
4. Run the compose `docker-compose up -d`

The negotiator will run using Postgres as DBMS and it will respond on `http://localhost:8080`.

Some [test data](https://github.com/BBMRI-ERIC/negotiator-v3/blob/master/negotiator-application/src/main/resources/data-postgres.sql) will also be added 

## References
