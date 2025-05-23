# REST API

The Negotiator features a RESTful(Representational State Transfer) API
designed according to the original [REST specification](https://ics.uci.edu/~fielding/pubs/dissertation/top.htm).
Some aspects of it also follow additional specifications and guidelines available
at [REST standards](https://standards.rest/).
The Current implementation is done using the [Spring framework](https://spring.io/projects/spring-framework)
in combination with the [Spring HATEOAS project](https://spring.io/projects/spring-hateoas).
Authentication and authorization documentation can be found [here](auth.md).

**Notice**

The API is currently undergoing major refactoring and rewrite, to make sure all
endpoints follow the same rules, and the design is consistent across the system.

## OpenAPI

Documentation for the REST API in OpenAPI format can be
found [here](https://negotiator-v3.bbmri-eric.eu/api/swagger-ui/index.html).