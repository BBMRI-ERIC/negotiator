# Webhooks API

The Negotiator uses webhooks to notify external systems when specific events happen.
Webhook messages are aligned with the [Standard Webhooks](https://www.standardwebhooks.com/)
guidelines and use HTTP `POST` requests with JSON payloads.

For the full interoperability specification, see the official
[Standard Webhooks specification](https://github.com/standard-webhooks/standard-webhooks/blob/main/spec/standard-webhooks.md).

## Event types and payloads

Event types follow a full-stop delimited format, for example `negotiation.state.updated`.

Negotiator sends a webhook JSON payload with this top-level structure:

- `type`: the event type identifier
- `timestamp`: when the event occurred (ISO 8601 UTC timestamp)
- `data`: event-specific fields

Example `ping` payload:

```json
{
	"type": "ping",
	"timestamp": "2026-04-29T10:15:30Z",
	"data": {
		"webhookId": 42
	}
}
```

All webhook operations and payload schemas are documented under the `Webhooks` section in the
[Negotiator OpenAPI documentation](https://negotiator-v3.bbmri-eric.eu/api/swagger-ui/index.html).

## Headers

Negotiator sends the standard webhook metadata headers (HTTP header names are case-insensitive). `Webhook-Signature` is included only when a webhook secret is configured:

| Header name         | Description                                              |
|---------------------|----------------------------------------------------------|
| `Webhook-Id`        | Unique message identifier. Useful as an idempotency key. |
| `Webhook-Timestamp` | Unix timestamp (seconds) for this delivery attempt.      |
| `Webhook-Signature` | Signature value(s) used to verify authenticity.          |

Example:

```http
Webhook-Id: msg_12345
Webhook-Timestamp: 1777457730
Webhook-Signature: v1,BASE64_SIGNATURE
```

## Webhook signatures

Negotiator signs webhook messages with HMAC-SHA256 when a webhook secret is configured.

Secret format:

- `whsec_<base64>`
- Base64 part must decode to 24-64 bytes

Signature input follows the Standard Webhooks format:

```text
webhook-id.webhook-timestamp.raw-body
```

Important verification rules:

- Verify against the raw request body bytes (do not parse and re-serialize JSON before verification)
- Enforce timestamp tolerance to reduce replay risk
- Use `Webhook-Id` as an idempotency key

Example using the official Standard Webhooks JavaScript reference library:

```javascript
import { Webhook } from "standardwebhooks"

const wh = new Webhook(process.env.NEGOTIATOR_WEBHOOK_SECRET)

wh.verify(rawBody, {
	"webhook-id": req.headers["webhook-id"],
	"webhook-timestamp": req.headers["webhook-timestamp"],
	"webhook-signature": req.headers["webhook-signature"],
})
```

Reference libraries are available for JavaScript/TypeScript, Java/Kotlin, Python, and other languages in the
Standard Webhooks repository.

## Managing webhooks

Webhooks are managed in the Negotiator UI by administrators.

1. Log in with administrator privileges.
2. Open **Admin Settings -> Webhooks**.
3. Add or edit a webhook with the following fields:
	 - **Webhook URL**: destination endpoint receiving HTTP POST requests.
	 - **Webhook Secret (optional)**: signing secret in the form `whsec_<base64>`.
		 Generate key material with:

		 ```bash
		 openssl rand -base64 32
		 ```

		 Then prefix the result with `whsec_`.
	 - **SSL Verification**: when enabled, TLS certificate validation is enforced.
	 - **Active**: enables or disables deliveries without deleting the webhook.

4. Use **Test** to send a `ping` delivery and validate endpoint configuration.

## Delivery History
The Delivery History tab shows recent delivery attempts for each webhook.

For each attempt, you can inspect:

- Delivery status (success/failure)
- Event type
- Delivery identifier
- Timestamp
- Response status code (if available)
- Error message (if delivery failed)
- Delivered JSON body

Deliveries are shown with newest first. Negotiator stores up to the latest 100 deliveries per webhook.

## Redeliveries

If a delivery failed or a consumer had downtime, you can trigger a manual redelivery from Delivery History.
To do that, click **Redeliver** on a delivery entry in the delivery history of a webhook.

Behavior:
- Reuses the same payload and event type as the selected source delivery
- Creates a new delivery attempt record
- Re-signs the request with current signing configuration
- Preserves root delivery identity for traceability and idempotent consumer handling

## Best practices

Follow these practices to improve security and reliability when receiving webhook deliveries.

- Use a webhook secret and verify the webhook signature for every request. See [Webhook signatures](#webhook-signatures).
- Keep SSL verification enabled. Disabling SSL verification should be limited to controlled local testing.
- Ensure your endpoint responds with a `2xx` status within 10 seconds.
- Design your receiver around the delivery time limits used by Negotiator:
	- connect timeout: 2 seconds
	- response timeout: 8 seconds
- Use the `Webhook-Id` header as an idempotency key:
	- store recently seen IDs for a short period
	- ignore duplicate deliveries to reduce replay risk
- Process webhook payloads asynchronously:
	- validate and acknowledge quickly
	- enqueue background processing
	- avoid long-running synchronous work before sending the `2xx` response