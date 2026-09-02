package eu.bbmri_eric.negotiator.lifecycle.evaluation;

/**
 * The params type of a strategy that takes none. Two of the four strategies ported here are in that
 * position: the parent-approval Guard and the terminal-aggregation Guard both ask a fixed question.
 *
 * <p>A marker type rather than a second, params-free strategy interface. Splitting {@code Guard} in
 * two would double the registry, double the fold, and make {@link GuardRegistry#bind} choose
 * between two catalogues on the strength of a row's {@code params} column being null — which is a
 * data fact, not a strategy fact. One interface with a type that carries nothing keeps a single
 * code path, and the cost is one empty record.
 *
 * <p>{@code null} params bind to {@link #INSTANCE} rather than to {@code null}, so a strategy body
 * never has to consider whether its own params argument is present.
 */
public record NoParams() {

  public static final NoParams INSTANCE = new NoParams();
}
