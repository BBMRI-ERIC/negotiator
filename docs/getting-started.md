# Quickstart

The Negotiator is divided between a standalone backend and a frontend exposing the user interface.
Since security is at the forefront the Negotiator needs an OIDC auth server to work properly. There is no other
supported mode of authentication.
To spin up a local instance of the Negotiator please run the following command.
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

If everything ran successfully, you should see 5 running containers. With the UI being available
on [port 8080](http://localhost:8080)
and API running on [port 8081](http://localhost:8081/api/swagger-ui/index.html). For guidance on how to deploy to
production, see [Deployment](/deployment).
For instructions on how to setup a development environment, see [Contributing](/contributing#development-environment).

- Traefik (for network orchestration)
- Negotiator backend
- PostgreSQL database
- Negotiator frontend
- [OIDC Server Mock](https://github.com/Soluto/oidc-server-mock)
