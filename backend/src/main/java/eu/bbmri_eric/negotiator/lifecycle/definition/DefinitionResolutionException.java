package eu.bbmri_eric.negotiator.lifecycle.definition;

/**
 * Definition Resolution could not name exactly one Definition Version for a piece of new work.
 *
 * <p>Resolution should always succeed, so this means the Definition Versions are misconfigured or
 * not seeded. It is thrown before anything is pinned or moved, so nothing is left half started.
 *
 * <p>The class is public so callers can tell it apart from other runtime failures. The constructor
 * is package-private so only resolution can throw it. Nothing maps it to an HTTP status, so a
 * caller gets a rolled-back 500.
 */
public class DefinitionResolutionException extends RuntimeException {

  DefinitionResolutionException(String message) {
    super(message);
  }
}
