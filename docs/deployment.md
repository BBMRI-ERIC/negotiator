# Deployment

The preferred deployment method is via Docker containers and Docker Compose. Manual deployment of a JAR file is also
possible, though it is not officially supported.

All our Docker containers run as non-root users, and the Dockerfiles are available publicly for validation.
Customization is handled through environment variables, more details are in the [Configuration](#configuration) section.

For all deployments, an OIDC authentication server is required, such as [Keycloak](https://www.keycloak.org/). Many life
science research infrastructures can alternatively use [LifeScience Login](https://lifescience-ri.eu/ls-login/) as their
OIDC provider. For eligibility and support, please [contact LifeScience Login](https://lifescience-ri.eu/ls-login/).

For more details on how the Negotiator manages OIDC connections, refer to [Authentication and Authorization](/auth).

## Docker Compose

For example, deployment using Docker Compose, see the
provided [Compose file](https://github.com/BBMRI-ERIC/negotiator/blob/docs/overhaul/compose.yml).
This example includes Traefik as a
reverse proxy.

Please note, however, that this Compose file is intended only for development and testing,
as it uses an OIDC server configured for non-production environments. For production setups, refer to the documentation
for recommended configurations.

## HTTPS

For enabling HTTPS you need to setup a reverse proxy
like [Traefik](https://doc.traefik.io/traefik/getting-started/install-traefik/) or [Nginx](https://nginx.org/en/).

```yaml
traefik:
  image: traefik:v3.2.0
  container_name: traefik
  network_mode: host  # Use host network
  command:
    - "--providers.docker=true"
    - "--entrypoints.websecure.address=:443"      # HTTPS entrypoint only
    - "--entrypoints.websecure.http.tls=true"     # Enable TLS for HTTPS
    - "--entrypoints.websecure.http.tls.certificates.certFile=/certs/cert.pem"
    - "--entrypoints.websecure.http.tls.certificates.keyFile=/certs/key.pem"
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock
    - ./certs:/certs  # Folder with provided SSL certificates
```

and adding following lables to any containers theat should be exposed:

```yaml
- "traefik.http.routers.negotiator-api.entrypoints=websecure"  # Only HTTPS
- "traefik.http.routers.negotiator-api.tls=true" 
```

## Configuration

Both the frontend and backend are customizable via environment variables, which can be specified in the Compose file.
Since the backend is a Spring application, all properties can also be passed as environment variables. For example, a
property can be defined as follows:

```yaml
logging:
  level:
    root: info
```

Can be passed to a docker container as:

```yaml
  negotiator:
    container_name: negotiator
    image: bbmrieric/negotiator:latest
    environment:
      - LOGGING_LEVEL_ROOT=info
```

To customize a Docker deployment, you can provide variables in the format mentioned above. For a comprehensive list of
properties you may want to modify, refer to
the [application file](https://github.com/BBMRI-ERIC/negotiator/blob/master/src/main/resources/application-prod.yaml).

### Custom properties

| Environment Variable                             | Description                                       | Default Value                                                                |
|--------------------------------------------------|---------------------------------------------------|------------------------------------------------------------------------------|
| `NEGOTIATOR_MOLGENISURL`                         | URL for the Molgenis service.                     | `""`                                                                         |
| `NEGOTIATOR_FRONTENDURL`                         | URL for the Negotiator frontend.                  | `""`                                                                         |
| `NEGOTIATOR_EMAIL_YOURSSINCERELYTEXT`            | Closing text for emails sent from the Negotiator. | `"The BBMRI-ERIC Team"`                                                      |
| `NEGOTIATOR_EMAIL_HELPDESKHREF`                  | Helpdesk email link.                              | `"mailto:negotiator@helpdesk.bbmri-eric.eu"`                                 |
| `NEGOTIATOR_EMAIL_LOGO`                          | URL for the Negotiator logo image.                | `"https://web.bbmri-eric.eu/Negotiator/2023-BBMRI-ERIC-Logo_NEGOTIATOR.png"` |
| `NEGOTIATOR_AUTHORIZATION_CLAIM`                 | Claim used for authorization.                     | `"eduperson_entitlement"`                                                    |
| `NEGOTIATOR_AUTHORIZATION_ADMINCLAIMVALUE`       | Claim value for admin users.                      | `""`                                                                         |
| `NEGOTIATOR_AUTHORIZATION_RESEARCHERCLAIMVALUE`  | Claim value for researcher users.                 | `""`                                                                         |
| `NEGOTIATOR_AUTHORIZATION_BIOBANKERCLAIMVALUE`   | Claim value for biobanker users.                  | `""`                                                                         |
| `NEGOTIATOR_NOTIFICATION_REMINDERCRONEXPRESSION` | Cron expression for reminder notifications.       | `"0 0 6 * * *"`                                                              |
| `NEGOTIATOR_EMAIL_FREQUENCYCRONEXPRESSION`       | Cron expression for email frequency.              | `"0 0 * * * *"`                                                              |

## Life science Login Integration

To
setup [LS Login](https://lifescience-ri.eu/ls-login/documentation/service-provider-documentation/service-provider-documentation.html)
as the OIDC provider you need to register a client that uses the **authorization_code flow with PKCE**.
If you also want to take advantage of Token introspection,
you will need to setup a second client for the backend that uses the **client_credentials flow** with similar settings
as the UI client except the flow.
To correctly parse User roles from the userinfo response you will need to setup respective groups
in [LS-Login IAM system](https://perun.aai.lifescience-ri.eu/home). Please refer to their documentation or support on
how to do that.
Below you can find examples configuration used by BBMRI-ERIC for the UI client:

**Redirect URLs**:

```
https://negotiator.bbmri-eric.eu/logged-in
https://negotiator.bbmri-eric.eu/api/swagger-ui/oauth2-redirect.html
```

**Scopes:**

```
openid
profile
email
offline_access
eduperson_entitlement
negotiator_api
eduperson_orcid
```

**Resource Indicators:**

```
https://negotiator.bbmri-eric.eu
```

