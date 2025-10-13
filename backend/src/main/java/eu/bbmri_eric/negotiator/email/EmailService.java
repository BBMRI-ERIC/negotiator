package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.user.Person;

/**
 * Service interface for sending email notifications from the Negotiator application.
 *
 * <p>This interface provides methods for sending emails to users, supporting both Person objects
 * (which contain user information) and direct email addresses. Implementations of this interface
 * should handle email formatting, delivery, and any necessary error handling.
 *
 * <p>The service is designed to be used throughout the Negotiator application for various
 * notification scenarios such as negotiation updates, user registration confirmations, and
 * administrative communications.
 *
 * @since 3.0
 */
public interface EmailService {

  /**
   * Sends an email to the specified Person recipient.
   *
   * <p>This method extracts the email address from the Person object and sends the email with the
   * provided subject and body content.
   *
   * @param recipient the Person object containing the recipient's information, including their
   *     email address; must not be null
   * @param subject the subject line of the email; must not be null or empty
   * @param mailBody the body content of the email in plain text or HTML format; must not be null
   * @param negotiationId if present, all the emails related to the same negotiationId will be
   *     grouped in the same thread
   * @param messageId unique identifier of the email message, useful to set thread header id
   * @throws IllegalArgumentException if any parameter is null or if subject/mailBody is empty
   * @throws RuntimeException if the email cannot be sent due to delivery issues
   */
  void sendEmail(
      Person recipient, String subject, String mailBody, String negotiationId, String messageId);

  /**
   * Sends an email to the specified email address.
   *
   * <p>This method sends an email directly to the provided email address without requiring a Person
   * object. Useful for sending emails to external recipients or when only the email address is
   * available.
   *
   * @param emailAddress the target email address; must not be null, empty, or malformed
   * @param subject the subject line of the email; must not be null or empty
   * @param message the message to be the core of the email, it will be auto formatted using default
   *     settings
   * @throws IllegalArgumentException if any parameter is null, empty, or if emailAddress is
   *     malformed
   * @throws RuntimeException if the email cannot be sent due to delivery issues
   */
  void sendEmail(String emailAddress, String subject, String message);
}
