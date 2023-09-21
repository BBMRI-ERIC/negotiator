# REST API

## Intro

Documentation for the REST API in OpenAPI format can be
found [here](https://negotiator-v3.bbmri-eric.eu/api/swagger-ui/index.html).

## Authentication and authorization

For handling authentication and authorization, we currently use LS AAI (an OpenID connect provider).
Using custom claims/group membership sent from LS AAI we dynamically handle resource level
permissions.