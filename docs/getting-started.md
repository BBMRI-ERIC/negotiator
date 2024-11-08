# Quickstart

The Negotiator is composed of a standalone backend and a frontend that provides the user interface. Security is a
priority, and the Negotiator requires an OIDC authentication server to operate correctly. No other authentication
methods are supported.

To launch a local instance of the Negotiator, run the following command:

> [!Warning] Prerequisites
> - Docker Engine 27.0 and newer
> - Docker Compose 2.30 and newer
> - Host networking enabled (See this [link](https://docs.docker.com/engine/network/drivers/host/) for Docker Desktop)
> - Unallocated Ports 8080, 8081 and 5432

```shell
git clone https://github.com/BBMRI-ERIC/negotiator.git
cd negotiator
docker compose up -d
```

If everything runs successfully, you should see 5 containers running. The UI will be accessible
at [port 8080](http://localhost:8080), and the API will be available
at [port 8081](http://localhost:8081/api/swagger-ui/index.html).

For production deployment guidance, refer to [Deployment](/deployment). For setup instructions for a development
environment, see [Contributing](/contributing#development-environment).

- Traefik (for network orchestration)
- Negotiator backend
- PostgreSQL database
- Negotiator frontend
- [OIDC Server Mock](https://github.com/Soluto/oidc-server-mock)

> [!TIP]
> The Database is available at the URL visible bellow with default credentials negotiator:negotiator:
> `jdbc:postgresql://localhost:5432/negotiator`