# Authentication and authorization

The Negotiator (both backend and frontend) uses [OAuth 2.0](https://oauth.net/2/)
and [OpenID Connect (OIDC)](https://openid.net/connect/) for secure authentication and authorization. These protocols
ensure that only authorized users and systems can access the resources provided by the Negotiator. The system supports
**Authorization Code Flow** for user authentication and **Client Credentials Flow** for machine-to-machine
communication,
allowing seamless integration in both interactive and automated scenarios.

## Authorization Code Flow (Frontend)

For user authentication, the frontend of the Negotiator uses the *
*[Authorization Code Flow](https://oauth.net/2/grant-types/authorization-code/)**, which is ideal for scenarios where a
user needs to log in via a third-party identity provider (such
as [Keycloak](https://www.keycloak.org/), [Google](https://developers.google.com/identity),
or [GitHub](https://docs.github.com/en/free-pro-team@latest/developers/apps/building-oauth-apps/creating-an-oauth-app)).
In this flow, the frontend application requests the user to authenticate with an authorization server, typically a
trusted identity provider. The flow works as follows:
![Diagram](/auth-code.png)
Credit for this diagram goes
to [Auth0](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow-with-pkce)
1. The frontend redirects the user to the OIDC provider (the authorization server) for authentication.
2. After the user successfully logs in, the OIDC provider redirects the user back to the frontend with an *
   *authorization code**.
3. The frontend exchanges the authorization code for an **access token** and, optionally, a **refresh token**, which can
   then be used to make authenticated API requests.

This flow is secure because the access token is never exposed to the user agent (browser), mitigating potential security
risks.
To retrieve additional user details, both the frontend and the backend call the
**[UserInfo endpoint](https://openid.net/specs/openid-connect-core-1_0.html#UserInfoEndpoint)**, which returns a JSON
object containing various claims about the authenticated user. These claims can include information like email, name,
roles, and custom attributes, which are essential for fine-grained authorization decisions.

Once the backend receives the _**userinfo**_ response, it parses the user's claims to make authorization decisions. The
roles and permissions assigned to a user are often based on specific claims returned in the **_userinfo_** response. For
instance, in the Negotiator platform, users may be assigned roles such as **RESEARCHER**, **ADMIN**, or
**REPRESENTATIVE** based on values found in a claim like **_eduperson_entitlement_**.

For example:

- If the *eduperson_entitlement* claim contains the value **RESEARCHER**, the user is granted the **RESEARCHER** role.
- Similarly, if the claim contains **ADMIN**, the user is granted the **ADMIN** role, and so on.

The values in the *eduperson_entitlement* claim (or any other claim) are fully customizable to suit the needs of your
application. You can define roles and permissions according to your organization's policies by mapping specific claim
values to roles in your system. The mapping can be customized in
the [application.yml file](https://github.com/BBMRI-ERIC/negotiator/blob/master/backend/src/main/resources/application.yaml).
or via environment variables.

This flexibility allows the Negotiator platform to adapt to various authorization schemes and ensures that users only
have access to resources and actions they are authorized to interact with.

For more information about how roles and claims are handled in OIDC, see the **[OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)** specification.

## Client Credentials Flow (Backend / External Systems)

The **[Client Credentials Flow](https://oauth.net/2/grant-types/client-credentials/)** is used when an external system
or a script needs to authenticate with the Negotiator backend without user involvement. This flow is typically used for
machine-to-machine interactions, such as when a script or a third-party service needs to access protected resources or
make API calls on behalf of a system (not a user). Here's how it works in a typical scenario:

1. An external system, like a script or an automation tool, needs to access data or perform operations within the
   Negotiator backend. To do this, the system first obtains its **client ID** and **client secret** from the OIDC
   provider (e.g., [Keycloak](https://www.keycloak.org/)).

2. The system sends a request to the OIDC authorization server's token endpoint, passing its client credentials (client
   ID and secret) to obtain an **access token**. This request does not involve any user authentication, as it is
   intended for machine-level access.

3. The OIDC provider responds with an **access token** that the external system can use to authenticate API requests to
   the Negotiator backend. The token is typically short-lived, and the system can request a new token using its client
   credentials once it expires.

For example, consider a script running on a server that needs to periodically pull data from the Negotiator backend (
e.g., to collect usage metrics or sync data with another system). Using the Client Credentials Flow, the script will
authenticate against the OIDC provider, obtain an access token, and use it to make API calls to the backend, all without
requiring user interaction.

This flow is ideal for automating tasks or integrating external systems that need access to the Negotiator's resources
in a secure and controlled manner.

## Token Validation (Backend)

Once the frontend or an external system obtains an access token using either the Authorization Code Flow or the Client
Credentials Flow, the backend of the Negotiator performs **token validation** before granting access to protected
resources. The backend validates the token by checking the following:

1. **Signature**: The backend verifies that the token's signature is valid and was issued by a trusted OIDC provider (
   authorization server). This ensures that the token has not been tampered with. The signature verification process
   typically uses the **[JWT](https://jwt.io/) (JSON Web Token)** standard, which defines how to create and verify the
   signature of tokens.

2. **Timestamp**: The backend checks the token's **exp** (expiration) claim to ensure the token is not expired. The
   backend may also verify the **nbf** (not before) claim to ensure the token is being used within the valid time frame.
   These claims help ensure that the token is still valid at the time of usage. For more information on these claims,
   refer to the **[JWT Claims](https://datatracker.ietf.org/doc/html/rfc7519#section-4.1)** specification.

3. **Audiences**: The backend validates the **aud** (audience) claim to ensure the token is intended for use with the
   Negotiator backend and not for some other service. The **aud** claim is used to specify the intended audience for the
   token. If the audience doesn't match, the token is rejected. For more details on audience validation, see the *
   *[JWT Audience Claim](https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.3)** specification.

4. **Introspection (Optional)**: **[Token Introspection](https://tools.ietf.org/html/rfc7662)** is an optional but
   highly recommended method for further validating the token. In this step, the backend can send a request to the OIDC
   provider's introspection endpoint to check if the token is still active and valid. This can help detect if the token
   has been revoked or is otherwise invalid, offering an additional layer of security. Introspection provides real-time
   information about the token's status and additional metadata (like its scopes and associated user info).

While the first three steps (signature, timestamp, and audiences) are mandatory, **token introspection** provides extra
assurance and can be enabled based on the security requirements of the environment.

## Authorization

In this seciton the roles supported by the Negotiator are described.
Unless stated otherwise, these roles are assigned through mapping values in a specific scope as stated above.

### Basic user roles

- **RESEARCHER**: The most basic role each user is granted. It allows the user to create Negotiations and interact with
  them.
- **REPRESENTATIVE**: A user responsible for mediating access to a given resource. It allows them interacting with
  relevant Negotiations.
- **NETWORK MANAGER**: A user responsible for moderating access to a group of resources. Grants them access to
  monitoring functionalities over relevant Negotiations.

### Special roles

- **ADMIN**: Administrator over the entire instance.
- **RESOURCE MANAGER**: Grants access to modify all available resources. Assigned to the user if the token contains
  scope _**negotiator_resource_management**_.
- **AUTH MANAGER**: Grants access to modify assigned representatives and network managers. Assigned to the user if the
  token contains scope _**negotiator_authz_management**_.
- **PROMETHEUS**: Grants access to metrics endpoints. Assigned to the user if the token contains scope
  _**negotiator_monitoring**_.

