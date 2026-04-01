package eu.bbmri_eric.negotiator.webhook;

import org.springframework.security.crypto.encrypt.TextEncryptor;

@FunctionalInterface
public interface WebhookTextEncryptorFactory {

  TextEncryptor create(String salt);
}
