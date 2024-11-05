# How to connect?

The Negotiator in itself is an access management system.
Therefore, it does not deal with the discovery or findability of
**resources** such as collections of biological samples.
This is the responsibility of the so-called **Discovery Services**.
Using a Discovery Service, the User is able to identify the resource that interests them, and to apply for
access, these resources need to be handed over to the Negotiator.

This handover is accomplished by an HTTP post request to the Negotiator REST API.
The specific endpoint can be found [here](https://negotiator.acc.bbmri-eric.eu/api/swagger-ui/index.html#/Requests/add).
Please refer to the detailed description in the visible Swagger UI.

> [!WARNING]  
> For the handover procedure to work your Discovery Service **must be registered** with the respective instance of the
> Negotiator.
> To do, that reach out to the administrators of that specific instance.