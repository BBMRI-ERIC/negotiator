package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;

/** Access verifier for individual Negotiations. */
public interface NegotiationAccessManager {
  /**
   * Verifies that the user has read access on public attributes of a Negotiation.
   *
   * @param negotiationId the ID of the Negotiation
   * @param userID the ID of the User
   * @throws eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException if the user does
   *     not have access.
   */
  void verifyReadAccessForNegotiation(String negotiationId, Long userID)
      throws ForbiddenRequestException;

  /**
   * Verifies that the user can update a Negotiation
   *
   * @param negotiationId the ID of the Negotiation
   * @param userID the ID of the User
   * @throws ForbiddenRequestException if the user does not have access
   */
  void verifyUpdateAccessForNegotiation(String negotiationId, Long userID)
      throws ForbiddenRequestException;
}
