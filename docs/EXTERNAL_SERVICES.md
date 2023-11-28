# External services

This document describes the external services that are used by the Negotiator.

## OpenID Provider

The Negotiator currently uses the [LifeScience login](https://lifescience-ri.eu/ls-login/) as the
default [OpenID](https://openid.net/) provider.
Configuration for the connection is in the `application.properties` file and parsing
of entitlements is implemented in `configuration/auth/JwtAuthenticationConverter.java`.
Note that the application should work with other OpenID providers,
such as Keycloak as well. However, this has not been tested.

## Molgenis

[Molgenis](https://www.molgenis.org/) is an open-source data platform for scientists and researchers. Several
instances of it such as the [BBMRI-ERIC Directory](https://directory.bbmri-eric.eu) use The Negotiator for submitting
and managing access requests. The communication works both ways
as the Negotiator has an interface to interact with the Molgenis API. The main purpose of it is to verify and to
retrieve resources/entities used in access requests. Implementation of the REST client can be found
at `MolgenisServiceImplementation.java`