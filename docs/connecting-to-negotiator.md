# How to connect?

The Negotiator functions as an access management system and does not handle the discovery or findability of **resources
** like collections of biological samples. This task is managed by dedicated **Discovery Services**.

With a Discovery Service, users can locate the resources they are interested in. Once identified, these resources are
then handed over to the Negotiator to proceed with access requests.

This handover is done via an HTTP POST request to the Negotiator's REST API. For details on the specific endpoint, refer
to [this link](https://negotiator.acc.bbmri-eric.eu/api/swagger-ui/index.html#/Requests/add) and consult the Swagger UI
for a comprehensive description.

> [!WARNING]  
> For the handover procedure to work your Discovery Service **must be registered** with the respective instance of the
> Negotiator.
> To do, that reach out to the administrators of that specific instance.