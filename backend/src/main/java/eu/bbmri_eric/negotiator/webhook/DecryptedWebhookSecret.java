package eu.bbmri_eric.negotiator.webhook;

/** Holds a decrypted webhook secret. Returned only once upon creation. */
public record DecryptedWebhookSecret(String id, String plainText) {}
