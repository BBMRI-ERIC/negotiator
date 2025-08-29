package eu.bbmri_eric.negotiator.common;

public interface AuthorizationService {

  /**
   * Checks if user is allowed to access resource.
   *
   * @param personId the id of the person to be checked for authorization.
   */
  void checkAuthorization(Long personId);
}
