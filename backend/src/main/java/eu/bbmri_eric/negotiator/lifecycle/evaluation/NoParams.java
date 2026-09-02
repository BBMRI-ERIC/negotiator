package eu.bbmri_eric.negotiator.lifecycle.evaluation;

/**
 * The params type of a strategy that takes none. It keeps one strategy interface and one code path,
 * instead of a second params-free interface and registry.
 *
 * <p>{@code null} params bind to {@link #INSTANCE}, so a strategy never receives {@code null}.
 */
public record NoParams() {

  public static final NoParams INSTANCE = new NoParams();
}
