package eu.bbmri_eric.negotiator.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Records every State and Event name that reaches the database as query text, so that "it builds"
 * stops being mistaken for evidence that this slab is done.
 *
 * <p>Fourteen names are spelled inside a query rather than referenced through a Java type. The
 * compiler sees none of them. Deleting the four Lifecycle enums produces no error at any of these
 * fourteen sites, and neither does renaming a State: the query keeps compiling, keeps running, and
 * silently matches nothing. That is the one failure mode this slab promised not to introduce, and
 * it is the only one the usual safety net cannot catch.
 *
 * <p>A grep would find them once and rot by the next slice, so the population is held here as a
 * standing fact instead. The list below is exact in all four coordinates - file, line, name and
 * spelling - which means a literal that is added, removed, renamed or merely pushed down by an
 * inserted import fails this test at the slice that caused it rather than at the close of the slab.
 * That is deliberate, and it is why this guard lands before slices 06 and 11 touch either file.
 *
 * <p><b>The scan looks for bare names, not for names inside quotes.</b> Thirteen of the fourteen
 * sit inside Java text blocks, where the name is either wrapped in SQL single quotes or in nothing
 * at all - never in the Java double quotes a naive {@code "DRAFT"} regex looks for. Such a regex
 * finds <em>zero</em> of the fourteen while reporting green, which is exactly the mistake the sweep
 * that produced this guard made first. So the scan reads the <em>content</em> of Java strings and
 * matches whole words inside it, and the SQL quoting, where there is any, is recorded rather than
 * required.
 *
 * <p><b>The vocabulary comes from the enums, not from a hand-kept list.</b> Test scope may still
 * name them, and deriving the forbidden words from {@code values()} is the only way this guard
 * cannot drift from what the Lifecycle actually calls its States and Events. At cutover the enums
 * go and this import stops compiling - loudly, at the moment the seed becomes the source of truth,
 * which is the right moment to decide what this guard should read instead.
 *
 * <p><b>This guard outlives the slab.</b> Two later slabs consume what it records. The migration
 * slab's seed has to satisfy every one of these fourteen names or the query behind a KPI starts
 * returning zero. ADR 0008 converts the audit table's {@code changed_to} column to a foreign key,
 * which breaks the six literals that filter on it - and one of those queries exists twice, in two
 * different repositories, so the same comparison has to be found in both places.
 *
 * <p><b>Nothing here changes a query.</b> The SQL is deliberately untouched by this whole slab;
 * this guard only writes down what is already true of it.
 *
 * <p>The scanning technique is borrowed from {@code CharacterizationImportGuardTest} and {@code
 * DefinitionInertnessGuardTest} - a working-directory-resolved scan root, a reader that ignores
 * comments so prose naming a literal is not itself a violation, and a violation report carrying
 * file and line. As slab 08 recorded for those two, the copy is deliberate: the three guards have
 * different lifetimes and each is meant to be deleted whole.
 */
class RawStateNamesInSqlGuardTest {

  private static final String PRODUCTION_SOURCE_PATH = "src/main/java";

  private static final String NEGOTIATOR_PACKAGE_PATH = "eu/bbmri_eric/negotiator";

  private static final String NETWORK_STATS_REPOSITORY =
      "eu/bbmri_eric/negotiator/governance/network/stats/NetworkStatsRepositoryImpl.java";

  private static final String NEGOTIATION_REPOSITORY =
      "eu/bbmri_eric/negotiator/negotiation/NegotiationRepository.java";

  /**
   * The lowest plausible size of this repository's production tree, well under today's 435 files. A
   * threshold this loose still fails the way that matters - a scan root that resolved to the wrong
   * directory, or a walk that silently returned nothing.
   */
  private static final int MINIMUM_PRODUCTION_SOURCES = 100;

  /** How a name is spelled where the query names it, because the two spellings fail differently. */
  private enum Spelling {
    /** Wrapped in SQL single quotes: a string constant the database compares against a column. */
    QUOTED,
    /**
     * Bare: an unquoted JPQL reference that Hibernate resolves against the enum-typed attribute it
     * is compared to. See {@link RawStateNamesInSqlGuardTest#UNQUOTED_JPQL_REFERENCE}.
     */
    BARE
  }

  /**
   * Where a query spells a State or Event name, and how. These four values are what the scan can
   * see and therefore what the exactness check compares on.
   *
   * @param file path under {@code src/main/java}, with {@code /} separators on every platform
   * @param line 1-based line in that file
   * @param name the State or Event the query names
   * @param spelling whether the query quotes the name or leaves it bare
   */
  private record Coordinates(String file, int line, String name, Spelling spelling)
      implements Comparable<Coordinates> {

    /** Same query, same name, same quoting - so a difference in {@code line} is a move. */
    private boolean isSameNameAs(Coordinates other) {
      return file.equals(other.file) && name.equals(other.name) && spelling == other.spelling;
    }

    @Override
    public int compareTo(Coordinates other) {
      int byFile = file.compareTo(other.file);
      int byLine = byFile != 0 ? byFile : Integer.compare(line, other.line);
      return byLine != 0 ? byLine : name.compareTo(other.name);
    }

    @Override
    public String toString() {
      return "%s:%d %s (%s)".formatted(file, line, name, spelling);
    }
  }

  /**
   * One pinned name, with the standing record of why it is there.
   *
   * @param coordinates where the query spells it, and how
   * @param reason which query it belongs to, what it filters, and what breaks it
   */
  private record PinnedName(Coordinates coordinates, String reason) {

    private PinnedName(String file, int line, String name, Spelling spelling, String reason) {
      this(new Coordinates(file, line, name, spelling), reason);
    }

    private String file() {
      return coordinates.file();
    }

    private int line() {
      return coordinates.line();
    }

    private String name() {
      return coordinates.name();
    }
  }

  /**
   * The one name in a query that is <em>not</em> a literal, kept apart because it fails in the
   * opposite way to the other thirteen.
   *
   * <p>{@code n.currentState != DRAFT} is JPQL, and Hibernate resolves the bare word against the
   * type of the attribute it is compared to - today {@code NegotiationState}, so it resolves to the
   * enum constant. The moment slice 11 makes {@code Negotiation.currentState} a {@code String},
   * that word has nothing left to resolve against and Hibernate rejects the query while it
   * validates the named queries, at application startup, before a single request is served.
   *
   * <p>That is the safe failure and the reason this one is worth naming separately: it is loud,
   * immediate, and impossible to ship. The thirteen quoted literals fail the other way - the query
   * still parses, still runs, and quietly matches no rows. If anyone ever "tidies" this line by
   * quoting it, it stops being the safe one, which is what {@link
   * #theUnquotedReference_isStillBare()} exists to notice.
   */
  private static final PinnedName UNQUOTED_JPQL_REFERENCE =
      new PinnedName(
          NEGOTIATION_REPOSITORY,
          74,
          "DRAFT",
          Spelling.BARE,
          "countAllNotDraftForNetwork - JPQL, a bare enum reference rather than a literal, excluding"
              + " drafts from the network's total. The only one of the fourteen whose breakage"
              + " is a startup failure rather than a silent zero.");

  /**
   * All fourteen names, each attributed to the query that holds it and to what breaks it. Thirteen
   * are SQL string constants; the fourteenth is {@link #UNQUOTED_JPQL_REFERENCE}, which is listed
   * here so the exactness check covers it and documented above because it fails differently.
   *
   * <p>Two hazards recur and are named in the reasons. <em>Audit column</em> marks a filter on
   * {@code negotiation_resource_lifecycle_record.changed_to}, which ADR 0008 converts to a foreign
   * key - six literals, in three queries, one of which is duplicated across two repositories.
   * <em>No production caller</em> marks a query only the repository tests reach today: still real
   * code, still broken by a bad seed, but broken without a user noticing.
   */
  private static final List<PinnedName> PINNED_NAMES =
      List.of(
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              28,
              "REPRESENTATIVE_CONTACTED",
              Spelling.QUOTED,
              "countIgnoredForNetwork - native, filters negotiation_resource_link.current_state for"
                  + " the ignored-negotiations KPI. No production caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              28,
              "REPRESENTATIVE_UNREACHABLE",
              Spelling.QUOTED,
              "countIgnoredForNetwork - native, the second half of the same current_state filter."
                  + " No production caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              51,
              "REPRESENTATIVE_CONTACTED",
              Spelling.QUOTED,
              "getIgnoredForNetwork - native, the same current_state filter as line 28, returning"
                  + " the ids behind numberOfIgnoredNegotiations."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              51,
              "REPRESENTATIVE_UNREACHABLE",
              Spelling.QUOTED,
              "getIgnoredForNetwork - native, the second half of that filter."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              75,
              "CHECKING_AVAILABILITY",
              Spelling.QUOTED,
              "getMedianResponseForNetwork - native, audit column. A duplicate of the query at "
                  + NEGOTIATION_REPOSITORY
                  + ":35, which is the copy the statistics service actually calls. No production"
                  + " caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              75,
              "RESOURCE_UNAVAILABLE",
              Spelling.QUOTED,
              "getMedianResponseForNetwork - native, audit column, second half of the same"
                  + " duplicated filter. No production caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              97,
              "RESOURCE_MADE_AVAILABLE",
              Spelling.QUOTED,
              "getNumberOfSuccessfulNegotiationsForNetwork - JPQL, filters rl.currentState for the"
                  + " successful-negotiations KPI. No production caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              119,
              "RESOURCE_MADE_AVAILABLE",
              Spelling.QUOTED,
              "getSuccessfulForNetwork - JPQL, the same currentState comparison as line 97,"
                  + " returning the ids behind numberOfSuccessfulNegotiations."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              163,
              "DRAFT",
              Spelling.QUOTED,
              "countStatusDistribution - JPQL, excludes drafts from the status distribution. The"
                  + " DRAFT visibility rule, in SQL rather than in a Specification."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              216,
              "REPRESENTATIVE_CONTACTED",
              Spelling.QUOTED,
              "getNumberOfActiveRepresentatives - native, audit column, excluding the states Spawn"
                  + " writes so that being spawned is not counted as being active."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              216,
              "REPRESENTATIVE_UNREACHABLE",
              Spelling.QUOTED,
              "getNumberOfActiveRepresentatives - native, audit column, second half of that"
                  + " exclusion."),
          new PinnedName(
              NEGOTIATION_REPOSITORY,
              35,
              "CHECKING_AVAILABILITY",
              Spelling.QUOTED,
              "getMedianResponseForNetwork - native, audit column, marking the first representative"
                  + " response. This is the copy NetworkStatisticsServiceImpl calls."),
          new PinnedName(
              NEGOTIATION_REPOSITORY,
              35,
              "RESOURCE_UNAVAILABLE",
              Spelling.QUOTED,
              "getMedianResponseForNetwork - native, audit column, second half of that filter."),
          UNQUOTED_JPQL_REFERENCE);

  /**
   * Every State and Event name the Lifecycle knows, taken from the enums rather than typed out. A
   * query naming any of them is what this guard is looking for; the four enums between them are the
   * complete vocabulary for as long as they exist.
   */
  private static final Set<String> LIFECYCLE_NAMES =
      Stream.of(
              Stream.of(NegotiationState.values()),
              Stream.of(NegotiationResourceState.values()),
              Stream.of(NegotiationEvent.values()),
              Stream.of(NegotiationResourceEvent.values()))
          .flatMap(names -> names)
          .map(Enum::name)
          .collect(Collectors.toCollection(LinkedHashSet::new));

  /**
   * What makes a Java string query text rather than prose. A string reaching the database says what
   * it selects and where from, so requiring both keywords separates the queries from the OpenAPI
   * examples, the {@code @Operation} descriptions and the exception messages that also name a State
   * - all of which name one legitimately and none of which the database ever sees.
   */
  private static final Pattern LOOKS_LIKE_A_QUERY =
      Pattern.compile(
          "\\bselect\\b.*\\bfrom\\b|\\binsert\\s+into\\b|\\bdelete\\s+from\\b"
              + "|\\bupdate\\b.*\\bset\\b",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  /**
   * A whole word inside query text, so that RESOURCE_UNAVAILABLE never matches inside the longer
   * RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT, and nrlr.changed_to never matches at all.
   */
  private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  @Test
  @DisplayName("the State names embedded in queries are exactly the fourteen recorded here")
  void queriesNameExactlyThePinnedStates() {
    List<Coordinates> found = scanProductionQueries();
    List<Coordinates> pinned = PINNED_NAMES.stream().map(PinnedName::coordinates).toList();

    List<Coordinates> unrecorded = withoutOneOccurrenceOfEach(found, pinned);
    List<Coordinates> vanished = withoutOneOccurrenceOfEach(pinned, found);
    if (unrecorded.isEmpty() && vanished.isEmpty()) {
      return;
    }
    fail(report(unrecorded, vanished));
  }

  @Test
  @DisplayName("every pinned name is on the line it claims, and says why it is there")
  void everyPinnedName_isWhereItSaysAndSaysWhy() {
    for (PinnedName pinned : PINNED_NAMES) {
      assertTrue(
          lineOf(pinned.file(), pinned.line()).contains(pinned.name()),
          """
          %s does not appear on %s:%d at all.

          This check reads the line directly rather than through the scan, so it catches a pin the
          scan cannot argue with: the file was rewritten and the coordinate now points at nothing."""
              .formatted(pinned.name(), pinned.file(), pinned.line()));

      assertFalse(
          pinned.reason().isBlank(),
          """
          %s has no reason recorded.

          A coordinate alone tells the migration slab where to look and nothing about what it is
          looking at. Say which query holds the name, what it filters, and what breaks it - the
          audit column ADR 0008 converts, or a KPI no production path reaches today."""
              .formatted(pinned.coordinates()));
    }
  }

  @Test
  @DisplayName("the one bare JPQL reference is still bare, and so still fails loudly")
  void theUnquotedReference_isStillBare() {
    String line = lineOf(UNQUOTED_JPQL_REFERENCE.file(), UNQUOTED_JPQL_REFERENCE.line());

    assertTrue(
        line.contains("!= " + UNQUOTED_JPQL_REFERENCE.name()),
        """
        %s:%d no longer compares against a bare %s.

        Line reads: %s

        This is the only one of the fourteen that fails safely. Hibernate resolves the bare word
        against the type of the attribute beside it, so once Negotiation.currentState is a String
        the query is rejected while the named queries are validated - at startup, before a request
        is served. Quoting the name turns that into a filter that parses, runs and matches nothing.

        If the comparison genuinely had to change, move this entry into PINNED_NAMES as a QUOTED
        literal and say in its reason why the loud failure was given up."""
            .formatted(
                UNQUOTED_JPQL_REFERENCE.file(),
                UNQUOTED_JPQL_REFERENCE.line(),
                UNQUOTED_JPQL_REFERENCE.name(),
                line.strip()));

    assertFalse(
        line.contains("'" + UNQUOTED_JPQL_REFERENCE.name() + "'"),
        "%s:%d has quoted the one reference whose whole value is that it is unquoted."
            .formatted(UNQUOTED_JPQL_REFERENCE.file(), UNQUOTED_JPQL_REFERENCE.line()));
  }

  @Test
  @DisplayName("the guard scans a real production tree and a rule that matches something")
  void guard_scansTheTreeItClaimsToPin() {
    List<Path> sources = sourceFiles();
    assertTrue(
        sources.size() >= MINIMUM_PRODUCTION_SOURCES,
        "Only %d production sources found under %s; the guard must never pass by scanning nothing."
            .formatted(sources.size(), productionRoot()));

    for (String file : List.of(NETWORK_STATS_REPOSITORY, NEGOTIATION_REPOSITORY)) {
      assertTrue(
          Files.isRegularFile(productionRoot().resolve(file)),
          """
          %s no longer exists. Every name this guard pins lives in it or in its sibling, so a
          rename leaves the guard reporting fourteen deletions and pinning nothing. Update the
          paths above to wherever the queries went."""
              .formatted(file));
    }

    assertFalse(
        LIFECYCLE_NAMES.isEmpty(),
        "The four Lifecycle enums contributed no names, so the scan is looking for nothing.");
    assertFalse(
        scanProductionQueries().isEmpty(),
        """
        The scan found no State name in any query, in a tree that holds fourteen of them. The walk,
        the string reader or the query rule has stopped working, and a green result from this guard
        would mean nothing.""");
  }

  @Test
  @DisplayName("the query rule separates query text from prose that names a State")
  void theQueryRule_matchesQueriesAndNotProse() {
    assertTrue(
        looksLikeAQuery("SELECT n.id FROM negotiation n WHERE n.current_state = 'DRAFT'"),
        "The rule must match a native query.");
    assertTrue(
        looksLikeAQuery("select count(n) from Negotiation n\n where n.currentState != DRAFT"),
        "The rule must match a query spread over several lines, as every text block here is.");
    assertTrue(
        looksLikeAQuery("update negotiation set current_state = 'ABANDONED'"),
        "The rule must match a write, or a seed correction could slip past it.");

    assertFalse(
        looksLikeAQuery("SUBMITTED"),
        "An OpenAPI example is a State name and nothing else, and names one legitimately.");
    assertFalse(
        looksLikeAQuery("The operation is allowed only if the negotiation is in DRAFT state"),
        "Prose describing an endpoint is not a query, even when it mentions a State.");
    assertFalse(
        looksLikeAQuery("Cannot delete a Negotiation that is not in DRAFT state"),
        "An exception message is not a query either.");
  }

  /**
   * Every State or Event name spelled inside query text anywhere in the production tree.
   *
   * <p>Runs of adjacent string literals joined by {@code +} are read as one query before the rule
   * is applied, because that is what they are: {@code NegotiationRepository} builds two of its
   * queries that way, and the chunk holding {@code != DRAFT} carries no {@code SELECT} of its own.
   * Matches are still reported at the line of the chunk they were found in.
   */
  private static List<Coordinates> scanProductionQueries() {
    List<Coordinates> found = new ArrayList<>();
    for (Path file : sourceFiles()) {
      String source = read(file);
      String relative = relativeToProductionRoot(file);
      for (List<StringLiteral> query : concatenatedRuns(source, stringLiterals(source))) {
        String text = query.stream().map(chunk -> chunk.content(source)).reduce("", String::concat);
        if (!looksLikeAQuery(text)) {
          continue;
        }
        for (StringLiteral chunk : query) {
          found.addAll(namesIn(source, chunk, relative));
        }
      }
    }
    return found;
  }

  private static List<Coordinates> namesIn(String source, StringLiteral chunk, String file) {
    List<Coordinates> found = new ArrayList<>();
    Matcher words = WORD.matcher(source).region(chunk.contentStart(), chunk.contentEnd());
    while (words.find()) {
      if (!LIFECYCLE_NAMES.contains(words.group())) {
        continue;
      }
      found.add(
          new Coordinates(
              file,
              lineNumberOf(source, words.start()),
              words.group(),
              spellingAt(source, words.start(), words.end())));
    }
    return found;
  }

  /**
   * {@code from} with one occurrence removed for each element of {@code subtract}, so that two
   * spellings of the same name on the same line cannot cancel each other out. {@code
   * List.removeAll} would remove both.
   */
  private static List<Coordinates> withoutOneOccurrenceOfEach(
      List<Coordinates> from, List<Coordinates> subtract) {
    List<Coordinates> remaining = new ArrayList<>(from);
    subtract.forEach(remaining::remove);
    return remaining.stream().sorted().toList();
  }

  /**
   * Quoted when the database sees quotes around the name, which is SQL's {@code '}, never Java's.
   */
  private static Spelling spellingAt(String source, int start, int end) {
    boolean quoted =
        start > 0
            && source.charAt(start - 1) == '\''
            && end < source.length()
            && source.charAt(end) == '\'';
    return quoted ? Spelling.QUOTED : Spelling.BARE;
  }

  private static boolean looksLikeAQuery(String text) {
    return LOOKS_LIKE_A_QUERY.matcher(text).find();
  }

  /**
   * The three things that can have happened, separated, because they need different answers.
   *
   * <p>A name that is unrecorded and one that has vanished, on the same query and with the same
   * spelling, are one literal that moved - almost always because a line was inserted above it. They
   * are paired first, so that a single added import does not present as eleven deletions and eleven
   * additions and leave the reader to diff two lists by eye.
   */
  private static String report(List<Coordinates> unrecorded, List<Coordinates> vanished) {
    List<Coordinates> added = new ArrayList<>(unrecorded);
    List<Coordinates> removed = new ArrayList<>(vanished);
    List<String> moved = new ArrayList<>();
    for (Coordinates from : vanished) {
      added.stream()
          .filter(from::isSameNameAs)
          .findFirst()
          .ifPresent(
              to -> {
                moved.add(
                    "%s:%d -> :%d %s (%s)"
                        .formatted(
                            from.file(), from.line(), to.line(), from.name(), from.spelling()));
                added.remove(to);
                removed.remove(from);
              });
    }

    StringBuilder message =
        new StringBuilder("The State names embedded in queries no longer match what is pinned.\n");
    section(
        message,
        "Moved - same query, same name, new line",
        moved,
        "Update the line number in PINNED_NAMES. Nothing else to decide.");
    section(
        message,
        "In a query but not pinned",
        added.stream().map(Coordinates::toString).toList(),
        """
        A query has started naming a State. Pin it: say which query it belongs to, whether it
        filters the audit column ADR 0008 converts to a foreign key, and whether any production
        path reaches it. The migration slab's seed will have to satisfy this name too.""");
    section(
        message,
        "Pinned but no longer there",
        removed.stream().map(Coordinates::toString).toList(),
        """
        A query has stopped naming a State. If that was deliberate, drop its entry here in the
        same change. If it was not, the query has quietly stopped filtering on anything and the
        number behind it has moved without anyone asking it to.""");

    return message
        .append(
            """

            What this guard must never be given is a looser rule. The compiler cannot see any of
            these names, so this list is the only place they are written down.""")
        .toString();
  }

  private static void section(
      StringBuilder message, String heading, List<String> entries, String remedy) {
    if (entries.isEmpty()) {
      return;
    }
    message.append('\n').append(heading).append(" (").append(entries.size()).append("):\n");
    entries.forEach(entry -> message.append("  ").append(entry).append('\n'));
    message.append(remedy.indent(2));
  }

  /**
   * One Java string literal's content, as offsets into the source; a text block is one of these.
   */
  private record StringLiteral(int rawStart, int contentStart, int contentEnd, int rawEnd) {
    private String content(String source) {
      return source.substring(contentStart, contentEnd);
    }
  }

  /**
   * The content of every string literal in a Java source, comments skipped so that prose naming a
   * State is not itself a finding, and character literals skipped so an apostrophe in code cannot
   * swallow the rest of the file.
   */
  private static List<StringLiteral> stringLiterals(String source) {
    List<StringLiteral> literals = new ArrayList<>();
    int index = 0;
    int length = source.length();
    while (index < length) {
      char current = source.charAt(index);
      if (current == '/' && source.startsWith("//", index)) {
        int end = source.indexOf('\n', index);
        index = end < 0 ? length : end;
      } else if (current == '/' && source.startsWith("/*", index)) {
        int end = source.indexOf("*/", index + 2);
        index = end < 0 ? length : end + 2;
      } else if (current == '\'') {
        index = endOfCharacterLiteral(source, index);
      } else if (current == '"' && source.startsWith("\"\"\"", index)) {
        int end = source.indexOf("\"\"\"", index + 3);
        end = end < 0 ? length : end;
        literals.add(new StringLiteral(index, index + 3, end, Math.min(end + 3, length)));
        index = Math.min(end + 3, length);
      } else if (current == '"') {
        int end = endOfStringLiteral(source, index);
        literals.add(new StringLiteral(index, index + 1, Math.min(end - 1, length), end));
        index = end;
      } else {
        index++;
      }
    }
    return literals;
  }

  private static int endOfCharacterLiteral(String source, int start) {
    int index = start + 1;
    while (index < source.length() && source.charAt(index) != '\'') {
      index += source.charAt(index) == '\\' ? 2 : 1;
    }
    return index + 1;
  }

  private static int endOfStringLiteral(String source, int start) {
    int index = start + 1;
    while (index < source.length() && source.charAt(index) != '"' && source.charAt(index) != '\n') {
      index += source.charAt(index) == '\\' ? 2 : 1;
    }
    return Math.min(index + 1, source.length());
  }

  /**
   * String literals grouped into the expressions they belong to: consecutive literals with nothing
   * but whitespace and {@code +} between them are one string as far as the database is concerned.
   */
  private static List<List<StringLiteral>> concatenatedRuns(
      String source, List<StringLiteral> literals) {
    List<List<StringLiteral>> runs = new ArrayList<>();
    List<StringLiteral> run = new ArrayList<>();
    StringLiteral previous = null;
    for (StringLiteral literal : literals) {
      if (previous != null && !isConcatenation(source, previous, literal)) {
        runs.add(run);
        run = new ArrayList<>();
      }
      run.add(literal);
      previous = literal;
    }
    if (!run.isEmpty()) {
      runs.add(run);
    }
    return runs;
  }

  private static boolean isConcatenation(
      String source, StringLiteral previous, StringLiteral next) {
    return BETWEEN_CONCATENATED_LITERALS
        .matcher(source.substring(previous.rawEnd(), next.rawStart()))
        .matches();
  }

  /** Nothing but whitespace and a {@code +} separates two halves of one concatenated string. */
  private static final Pattern BETWEEN_CONCATENATED_LITERALS = Pattern.compile("[\\s+]*");

  private static int lineNumberOf(String source, int offset) {
    int line = 1;
    for (int index = 0; index < offset; index++) {
      if (source.charAt(index) == '\n') {
        line++;
      }
    }
    return line;
  }

  private static String lineOf(String file, int line) {
    List<String> lines = readLines(productionRoot().resolve(file));
    assertTrue(
        line <= lines.size(),
        "%s has only %d lines, so it cannot hold the pin at line %d."
            .formatted(file, lines.size(), line));
    return lines.get(line - 1);
  }

  private static String relativeToProductionRoot(Path file) {
    return productionRoot().relativize(file).toString().replace(java.io.File.separatorChar, '/');
  }

  private static List<Path> sourceFiles() {
    try (Stream<Path> tree = Files.walk(productionRoot())) {
      return tree.filter(Files::isRegularFile)
          .filter(file -> file.getFileName().toString().endsWith(".java"))
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * The production tree, resolved from the working directory rather than the classpath: the rule is
   * about source text, and a compiled class no longer shows how a query was spelled. Surefire runs
   * with the module directory as working directory; the walk upwards keeps the guard working when
   * tests are launched from the repository root or from an IDE.
   *
   * <p>Only {@code src/main/java} is scanned. The Flyway migrations under {@code
   * src/main/resources} name plenty of States in their check constraints, but a landed migration is
   * immutable by checksum - it cannot move, and pinning it would say nothing a reader could act on.
   */
  private static Path moduleRoot() {
    Path start = Path.of("").toAbsolutePath();
    for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
      for (String modulePrefix : List.of("", "backend")) {
        Path module = candidate.resolve(modulePrefix);
        if (Files.isDirectory(
            module.resolve(PRODUCTION_SOURCE_PATH).resolve(NEGOTIATOR_PACKAGE_PATH))) {
          return module;
        }
      }
    }
    throw new IllegalStateException(
        ("Could not locate a module holding %s/%s from working directory %s."
                + " The guard must never pass by finding nothing.")
            .formatted(PRODUCTION_SOURCE_PATH, NEGOTIATOR_PACKAGE_PATH, start));
  }

  private static Path productionRoot() {
    return moduleRoot().resolve(PRODUCTION_SOURCE_PATH);
  }

  private static String read(Path file) {
    return String.join("\n", readLines(file));
  }

  private static List<String> readLines(Path file) {
    try {
      return Files.readAllLines(file);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read " + file.toAbsolutePath(), e);
    }
  }
}
