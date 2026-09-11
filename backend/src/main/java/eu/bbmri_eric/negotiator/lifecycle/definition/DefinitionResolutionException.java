package eu.bbmri_eric.negotiator.lifecycle.definition;

/**
 * Definition Resolution could not name exactly one Lifecycle Definition for a piece of new work.
 *
 * <p>Unchecked, and deliberately not a case a caller is expected to handle: resolution is meant to
 * be total, so an unresolvable scope is a misconfigured or unseeded set of Definition Versions
 * rather than an outcome. It is thrown before anything is pinned or moved, so nothing is left half
 * initialized.
 *
 * <p><b>Public so that it can be caught, package private so that only resolution can throw it.</b>
 * The class had to leave the package with {@link LifecycleDefinitions#resolveForNegotiation()}: an
 * exception a caller cannot name is one it cannot distinguish from every other runtime failure, and
 * the six definition tables are empty in every environment today, so this is the <em>expected</em>
 * answer rather than a remote one. Its constructor stays package private, because nothing outside
 * is in a position to have failed at resolution.
 *
 * <p>What it still does not say is what a caller should <em>do</em>. Whether an unresolvable
 * definition fails a Negotiation approval, and with which status, is a decision the map files with
 * the coupling slab; today's default is a rolled-back 500, and nothing here maps it to anything
 * else.
 */
public class DefinitionResolutionException extends RuntimeException {

  DefinitionResolutionException(String message) {
    super(message);
  }
}
