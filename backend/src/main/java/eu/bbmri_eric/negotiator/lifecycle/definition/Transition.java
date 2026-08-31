package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.RequiredAuthority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * One edge of a Lifecycle Definition's graph: firing {@code event} while in {@code fromState} moves
 * to {@code toState}, if the caller holds {@code requiredAuthority} and the Guard chain permits it.
 *
 * <p>All four references are immutable — re-pointing any of them does not edit this edge, it makes
 * a different one. {@code requiredAuthority} is the one attribute that is not part of the edge's
 * shape, so it is the one thing that can be edited.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
class Transition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Stored rather than derived through {@code fromState}, because the evaluator loads a whole graph
   * by its Definition Version id. The database enforces that it agrees with all three of the
   * references below.
   */
  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lifecycle_definition_id", nullable = false, updatable = false)
  private LifecycleDefinition lifecycleDefinition;

  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_state_id", nullable = false, updatable = false)
  private State fromState;

  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_state_id", nullable = false, updatable = false)
  private State toState;

  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false, updatable = false)
  private Event event;

  /** Never null: {@link RequiredAuthority#NONE} is how "anyone may fire this" is spelled. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RequiredAuthority requiredAuthority;
}
