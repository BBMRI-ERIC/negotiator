package eu.bbmri_eric.negotiator.lifecycle.definition;

/**
 * Definition Resolution could not name exactly one Lifecycle Definition for a piece of new work.
 *
 * <p>Unchecked, and deliberately not a case a caller is expected to handle: resolution is meant to
 * be total, so an unresolvable scope is a misconfigured or unseeded set of Definition Versions
 * rather than an outcome. It is thrown before anything is pinned or moved, so nothing is left half
 * initialized.
 */
class DefinitionResolutionException extends RuntimeException {

  DefinitionResolutionException(String message) {
    super(message);
  }
}
