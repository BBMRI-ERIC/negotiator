package eu.bbmri_eric.negotiator.lifecycle.definition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The Wiring that attaches a Guard to one Transition, or to a whole Definition Version when {@code
 * transition} is null. The two scopes have separate {@code sort_order} sequences.
 *
 * <p>{@code params} holds only configuration. Runtime domain state reaches a Guard through the
 * evaluation context at fire time.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
class GuardWiring {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lifecycle_definition_id", nullable = false, updatable = false)
  private LifecycleDefinition lifecycleDefinition;

  /**
   * Null means the Guard applies to every Transition of the Definition Version. Moving a Guard to
   * another scope makes a new Wiring row.
   */
  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transition_id", updatable = false)
  private Transition transition;

  /** Names a Java strategy from a fixed catalogue; changing it makes a different wiring. */
  @Setter(AccessLevel.NONE)
  @Column(nullable = false, updatable = false)
  private String typeKey;

  /** Per-strategy configuration. Null is legal: a strategy that takes no parameters needs none. */
  @JdbcTypeCode(SqlTypes.JSON)
  private String params;

  /** Ordering within the Guard's scope. Each scope has its own sequence. */
  @Column(nullable = false)
  private Integer sortOrder;
}
