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
 * The configuration attaching a Guard to where it applies — one Transition, or an entire Definition
 * Version and therefore all of its Transitions. The latter is spelled as a null {@code transition},
 * and the two scopes have independent {@code sort_order} sequences.
 *
 * <p>{@code typeKey} names a Java strategy from a fixed catalogue, and {@code params} carries that
 * strategy's per-wiring configuration as jsonb. Runtime domain state never travels in {@code
 * params} — it reaches a strategy through the evaluation context at fire time.
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
   * Null means the Guard applies to every Transition of the Definition Version; set means that
   * Transition alone. Immutable: re-pointing a Guard from one scope to another does not edit this
   * wiring, it makes a different one.
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

  /** Ordering within the Guard's scope — definition-wide or transition-scoped, independently. */
  @Column(nullable = false)
  private Integer sortOrder;
}
