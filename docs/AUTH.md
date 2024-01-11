# Authentication and authorization

For Authentication and Authorization, the API utilizes the OAuth2 and OpenID protocols.
The configuration such as the authorization server URLs and role mappings can be edited in
the [Spring properties file](../src/main/resources/application.yaml).
Currently,
the application supports the standard [authorization-code flow ](https://oauth.net/2/grant-types/authorization-code/)
for requests by regular users
and the [client-credentials flow](https://oauth.net/2/grant-types/client-credentials/) for machine-to-machine
communication.
However, the Authorization server **must provide the json web token** (JWT) in the response **not an opaque token**.

## OpenID Provider

The Negotiator currently uses the [LifeScience login](https://lifescience-ri.eu/ls-login/) as the
default [OpenID](https://openid.net/) provider.
Configuration for the connection is in the `application.properties` file and parsing
of entitlements is implemented in `configuration/auth/JwtAuthenticationConverter.java`.
Note that the application should work with other OpenID providers,
such as Keycloak as well. However, this has not been tested.

