package eu.bbmri_eric.negotiator.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Comparator;
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
 * Pins every State name that production code writes into query text. The scan also looks for Event
 * names, and finds none.
 *
 * <p>Fourteen names sit inside queries, where the compiler cannot see them. Deleting the Lifecycle
 * enums or renaming a State breaks none of them at compile time. The query keeps running and
 * matches nothing.
 *
 * <p>The list records file, line, name and spelling exactly. Adding, removing or renaming a
 * literal, or shifting it with an inserted line, fails this test in the change that caused it.
 *
 * <p>The scan looks for bare names, not names inside Java quotes. Thirteen of the fourteen sit in
 * Java text blocks, either in SQL single quotes or unquoted, so a regex for {@code "DRAFT"} finds
 * none of them. The scan reads the content of Java strings and matches whole words in it, and
 * records the SQL quoting where there is any.
 *
 * <p>The names come from the enums' {@code values()}, so the guard cannot drift from what the
 * Lifecycle calls its States and Events. At cutover the enum imports stop compiling, and that is
 * the time to decide what this guard reads instead.
 *
 * <p>Two later changes depend on this list. The seed must contain all fourteen names, or a KPI
 * query returns zero. Turning the Lifecycle Record's {@code changed_to} column into a foreign key
 * breaks the six literals that filter on it, and one of those queries exists in two repositories.
 *
 * <p>This guard changes no query. It only records them.
 *
 * <p>The scanning code is copied between the guard tests on purpose. The guards have different
 * lifetimes and each is deleted whole.
 */
class RawStateNamesInSqlGuardTest {

  private static final String PRODUCTION_SOURCE_PATH = "src/main/java";

  private static final String NEGOTIATOR_PACKAGE_PATH = "eu/bbmri_eric/negotiator";

  private static final String NETWORK_STATS_REPOSITORY =
      "eu/bbmri_eric/negotiator/governance/network/stats/NetworkStatsRepositoryImpl.java";

  private static final String NEGOTIATION_REPOSITORY =
      "eu/bbmri_eric/negotiator/negotiation/NegotiationRepository.java";

  /**
   * Well below the real size of the production tree. It still fails when the scan root resolves to
   * the wrong directory or the walk returns nothing.
   */
  private static final int MINIMUM_PRODUCTION_SOURCES = 100;

  /** How a query spells a name. The two spellings fail differently. */
  private enum Spelling {
    /** Wrapped in SQL single quotes: a string constant the database compares against a column. */
    QUOTED,
    /**
     * Bare: an unquoted JPQL word, which Hibernate resolves against the type of the attribute it is
     * compared to. No query uses it, and {@link
     * RawStateNamesInSqlGuardTest#noQuerySpellsANameBare()} reports one that does.
     */
    BARE
  }

  /**
   * Where a query spells a State or Event name, and how.
   *
   * @param file path under {@code src/main/java}, with {@code /} separators on every platform
   * @param line 1-based line in that file
   * @param name the State or Event the query names
   * @param spelling whether the query quotes the name or leaves it bare
   */
  private record Coordinates(String file, int line, String name, Spelling spelling)
      implements Comparable<Coordinates> {

    /** Same file, name and quoting, so a different {@code line} means the literal moved. */
    private boolean isSameLiteralAs(Coordinates other) {
      return file.equals(other.file) && name.equals(other.name) && spelling == other.spelling;
    }

    private int linesAwayFrom(Coordinates other) {
      return Math.abs(line - other.line);
    }

    /** Ordered on every component, so that ordering and {@code equals} cannot disagree. */
    @Override
    public int compareTo(Coordinates other) {
      int byFile = file.compareTo(other.file);
      if (byFile != 0) {
        return byFile;
      }
      int byLine = Integer.compare(line, other.line);
      if (byLine != 0) {
        return byLine;
      }
      int byName = name.compareTo(other.name);
      return byName != 0 ? byName : spelling.compareTo(other.spelling);
    }

    @Override
    public String toString() {
      return "%s:%d %s (%s)".formatted(file, line, name, spelling);
    }
  }

  /**
   * One pinned name and why it is there.
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
   * All fourteen names, each with the query that holds it and what breaks it. All fourteen are SQL
   * string constants, so this list is the only place they are written down.
   *
   * <p>Two hazards recur in the reasons. <em>Audit column</em> marks a filter on {@code
   * negotiation_resource_lifecycle_record.changed_to}, which becomes a foreign key at cutover.
   * Six literals in three queries filter on it, and one of those queries exists in two
   * repositories. <em>No production caller</em> marks a query that only repository tests reach. A
   * bad seed still breaks it, but no user notices.
   */
  private static final List<PinnedName> PINNED_NAMES =
      List.of(
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              28,
              "REPRESENTATIVE_CONTACTED",
              Spelling.QUOTED,
              "countIgnoredForNetwork, native, filters negotiation_resource_link.current_state for"
                  + " the ignored-negotiations KPI. No production caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              28,
              "REPRESENTATIVE_UNREACHABLE",
              Spelling.QUOTED,
              "countIgnoredForNetwork, native, the second half of the same current_state filter."
                  + " No production caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              51,
              "REPRESENTATIVE_CONTACTED",
              Spelling.QUOTED,
              "getIgnoredForNetwork, native, the same current_state filter as line 28, returning"
                  + " the ids behind numberOfIgnoredNegotiations."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              51,
              "REPRESENTATIVE_UNREACHABLE",
              Spelling.QUOTED,
              "getIgnoredForNetwork, native, the second half of that filter."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              75,
              "CHECKING_AVAILABILITY",
              Spelling.QUOTED,
              "getMedianResponseForNetwork, native, audit column. A duplicate of the query at "
                  + NEGOTIATION_REPOSITORY
                  + ":35, which is the copy the statistics service actually calls. No production"
                  + " caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              75,
              "RESOURCE_UNAVAILABLE",
              Spelling.QUOTED,
              "getMedianResponseForNetwork, native, audit column, second half of the same"
                  + " duplicated filter. No production caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              97,
              "RESOURCE_MADE_AVAILABLE",
              Spelling.QUOTED,
              "getNumberOfSuccessfulNegotiationsForNetwork, JPQL, filters rl.currentState for the"
                  + " successful-negotiations KPI. No production caller."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              119,
              "RESOURCE_MADE_AVAILABLE",
              Spelling.QUOTED,
              "getSuccessfulForNetwork, JPQL, the same currentState comparison as line 97,"
                  + " returning the ids behind numberOfSuccessfulNegotiations."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              163,
              "DRAFT",
              Spelling.QUOTED,
              "countStatusDistribution, JPQL, excludes drafts from the status distribution. The"
                  + " DRAFT visibility rule, in SQL rather than in a Specification."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              216,
              "REPRESENTATIVE_CONTACTED",
              Spelling.QUOTED,
              "getNumberOfActiveRepresentatives, native, audit column, excluding the states Spawn"
                  + " writes so that being spawned is not counted as being active."),
          new PinnedName(
              NETWORK_STATS_REPOSITORY,
              216,
              "REPRESENTATIVE_UNREACHABLE",
              Spelling.QUOTED,
              "getNumberOfActiveRepresentatives, native, audit column, second half of that"
                  + " exclusion."),
          new PinnedName(
              NEGOTIATION_REPOSITORY,
              33,
              "CHECKING_AVAILABILITY",
              Spelling.QUOTED,
              "getMedianResponseForNetwork, native, audit column, marking the first representative"
                  + " response. This is the copy NetworkStatisticsServiceImpl calls."),
          new PinnedName(
              NEGOTIATION_REPOSITORY,
              33,
              "RESOURCE_UNAVAILABLE",
              Spelling.QUOTED,
              "getMedianResponseForNetwork, native, audit column, second half of that filter."),
          new PinnedName(
              NEGOTIATION_REPOSITORY,
              71,
              "DRAFT",
              Spelling.QUOTED,
              "countAllNotDraftForNetwork, JPQL, excluding drafts from the network's total. The"
                  + " attribute it is compared to is a String, so the name must be quoted, and it"
                  + " fails as silently as the other thirteen."));

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
   * Tells query text from prose. Requiring both keywords of a statement separates queries from the
   * OpenAPI examples, {@code @Operation} descriptions and exception messages that also name a
   * State.
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

          A coordinate alone says where to look and nothing about what is there. Say which query
          holds the name, what it filters, and what breaks it, such as the audit column that is
          converted to a foreign key at cutover, or a KPI no production path reaches."""
              .formatted(pinned.coordinates()));
    }
  }

  @Test
  @DisplayName("no query spells a State name as a bare word any more")
  void noQuerySpellsANameBare() {
    List<Coordinates> bare =
        scanProductionQueries().stream()
            .filter(found -> found.spelling() == Spelling.BARE)
            .toList();

    assertTrue(
        bare.isEmpty(),
        """
        A query spells a State or Event name as a bare word: %s

        No compared attribute is enum-typed, so a bare name has nothing to resolve against.
        Hibernate rejects the query when it validates the named queries at startup, before any
        request is served.

        Quote the name if the comparison is meant to be against a literal, or bind it as a
        parameter from a Well-known name holder if it is meant to be visible to the compiler."""
            .formatted(bare));

    assertFalse(
        PINNED_NAMES.stream().anyMatch(pinned -> pinned.coordinates().spelling() == Spelling.BARE),
        """
        A pinned name is recorded as BARE while the scan finds none, so the record and the tree
        disagree about the one distinction this guard draws between them.""");
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
  @DisplayName("a scan root that resolves wrongly refuses rather than scanning nothing")
  void resolvingTheScanRoot_failsRatherThanFindingNothing() {
    IllegalStateException refused =
        assertThrows(
            IllegalStateException.class,
            () -> moduleHolding("src/main/nowhere", NEGOTIATOR_PACKAGE_PATH),
            """
            Asked for a source directory that does not exist, the resolution step returned a path
            instead of refusing. Everything this guard reports is relative to that path, so a wrong
            one walks an empty tree, finds no literal, and reports green over a codebase it never
            looked at.""");
    assertTrue(
        refused.getMessage().contains("src/main/nowhere"),
        "The refusal must name the path it could not find, or it cannot be acted on: "
            + refused.getMessage());

    assertEquals(
        moduleRoot().resolve(PRODUCTION_SOURCE_PATH),
        productionRoot(),
        "The real scan root must still resolve through the same step the wrong one was refused by.");
  }

  @Test
  @DisplayName("the spelling detector tells a quoted name from a bare one")
  void theSpellingDetector_distinguishesQuotedFromBare() {
    assertEquals(
        Spelling.QUOTED,
        spellingOfTheNameIn("where n.current_state = 'DRAFT' and x = 1"),
        "A name inside SQL quotes is a literal the database compares against.");
    assertEquals(
        Spelling.BARE,
        spellingOfTheNameIn("where n.currentState != DRAFT and x = 1"),
        """
        A name with no quotes around it reads as QUOTED, so the detector cannot tell the two apart.

        Every other check here runs through this method. The tree holds no bare name, so a
        detector stuck on QUOTED would leave both the exactness check and the no-bare-names check
        green over a query the database will reject.""");
  }

  private static Spelling spellingOfTheNameIn(String query) {
    Matcher words = WORD.matcher(query);
    while (words.find()) {
      if (LIFECYCLE_NAMES.contains(words.group())) {
        return spellingAt(query, words.start(), words.end());
      }
    }
    return fail("No Lifecycle name in: " + query);
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
   * <p>String literals joined by {@code +} are read as one query, because {@code
   * NegotiationRepository} builds two queries that way and the chunk holding {@code != DRAFT} has
   * no {@code SELECT} of its own. Each match is reported at the line of its own chunk.
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
   * Splits the mismatch into moved, added and removed names, because each needs a different fix.
   *
   * <p>An unrecorded name and a vanished name with the same file, name and spelling are one literal
   * that moved, usually because a line was inserted above it. Pairing them first keeps one added
   * import from showing up as eleven deletions and eleven additions.
   *
   * <p>Pairing picks the nearest line. {@code REPRESENTATIVE_CONTACTED} appears three times in one
   * file, so pairing the first match could report line 28 as moved to line 216.
   */
  private static String report(List<Coordinates> unrecorded, List<Coordinates> vanished) {
    List<Coordinates> added = new ArrayList<>(unrecorded);
    List<Coordinates> removed = new ArrayList<>(vanished);
    List<String> moved = new ArrayList<>();
    for (Coordinates from : vanished) {
      added.stream()
          .filter(from::isSameLiteralAs)
          .min(Comparator.comparingInt(from::linesAwayFrom))
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
        "Moved: same query, same name, new line",
        moved,
        "Update the line number in PINNED_NAMES. Nothing else to decide.");
    section(
        message,
        "In a query but not pinned",
        added.stream().map(Coordinates::toString).toList(),
        """
        A query has started naming a State. Pin it: say which query it belongs to, whether it
        filters the audit column that becomes a foreign key at cutover, and whether any
        production path reaches it. The seed will have to contain this name too.""");
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

            Do not loosen this rule. The compiler cannot see any of these names, so this list is
            the only place they are written down.""")
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
   * The content of every string literal in a Java source. It skips comments, so a comment naming a
   * State is not a finding, and character literals, so an apostrophe cannot swallow the rest of the
   * file.
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
   * The production tree, resolved from the working directory instead of the classpath, because the
   * rule reads source text. Surefire runs in the module directory, and walking upwards keeps the
   * guard working from the repository root or an IDE.
   *
   * <p>Only {@code src/main/java} is scanned. The Flyway migrations name States in their check
   * constraints, but Flyway checksums forbid changing an applied migration, so pinning them tells
   * nobody anything to act on.
   */
  private static Path moduleRoot() {
    return moduleHolding(PRODUCTION_SOURCE_PATH, NEGOTIATOR_PACKAGE_PATH);
  }

  /**
   * Takes both path parts as arguments so that {@link
   * #resolvingTheScanRoot_failsRatherThanFindingNothing()} can ask for a missing directory and
   * check that it throws.
   */
  private static Path moduleHolding(String sourcePath, String packagePath) {
    Path start = Path.of("").toAbsolutePath();
    for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
      for (String modulePrefix : List.of("", "backend")) {
        Path module = candidate.resolve(modulePrefix);
        if (Files.isDirectory(module.resolve(sourcePath).resolve(packagePath))) {
          return module;
        }
      }
    }
    throw new IllegalStateException(
        ("Could not locate a module holding %s/%s from working directory %s."
                + " The guard must never pass by finding nothing.")
            .formatted(sourcePath, packagePath, start));
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
