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

/**
 * A named position within one Lifecycle Definition's graph. Its {@code name} is the natural key a
 * live state string resolves through; its {@code label} is what a reader sees.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
class State {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** A State belongs to its Definition Version for good; re-homing one would alter two graphs. */
  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lifecycle_definition_id", nullable = false, updatable = false)
  private LifecycleDefinition lifecycleDefinition;

  /**
   * The natural key a live state string resolves through, so it never moves: renaming a State would
   * silently re-point every string that resolves through it. A rename is a new Definition Version.
   */
  @Setter(AccessLevel.NONE)
  @Column(nullable = false, updatable = false)
  private String name;

  /** Rendered by the UI and by the Resource state-change notification body. */
  @Column(nullable = false)
  private String label;

  /** Where a Lifecycle starts. At most one State per Definition Version carries it. */
  @Builder.Default
  @Column(nullable = false)
  private boolean initial = false;

  /** Where a Lifecycle is finished. Any number of States may carry it. */
  @Builder.Default
  @Column(nullable = false)
  private boolean terminal = false;
}
