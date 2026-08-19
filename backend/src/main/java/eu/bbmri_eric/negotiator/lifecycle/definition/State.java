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
 * A named position within one Definition Version's graph. A live State string resolves through
 * {@code name}. People see {@code label}.
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

  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lifecycle_definition_id", nullable = false, updatable = false)
  private LifecycleDefinition lifecycleDefinition;

  /**
   * The key a live State string resolves through. Renaming it would re-point every such string, so a
   * rename needs a new Definition Version.
   */
  @Setter(AccessLevel.NONE)
  @Column(nullable = false, updatable = false)
  private String name;

  /** Shown in the UI and in the Resource State change notification. */
  @Column(nullable = false)
  private String label;

  @Builder.Default
  @Column(nullable = false)
  private boolean initial = false;

  @Builder.Default
  @Column(nullable = false)
  private boolean terminal = false;
}
