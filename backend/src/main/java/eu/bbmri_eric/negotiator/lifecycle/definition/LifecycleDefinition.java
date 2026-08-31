package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.DefinitionScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One immutable Definition Version of a Definition Family: a complete graph of States, Events and
 * Transitions, identified by its row id alone. Only {@code name}, {@code active} and {@code
 * globalDefault} are editable.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
class LifecycleDefinition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Fixed for the whole Definition Family, though nothing enforces that across its rows. */
  @Setter(AccessLevel.NONE)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private DefinitionScope scope;

  @Setter(AccessLevel.NONE)
  @Column(nullable = false, updatable = false)
  private String familyKey;

  @Column(nullable = false)
  private String name;

  @Setter(AccessLevel.NONE)
  @Column(nullable = false, updatable = false)
  private Integer version;

  @Builder.Default
  @Column(nullable = false)
  private boolean active = false;

  /** Marks the Global Default Family; stored per row as there is no family table. */
  @Builder.Default
  @Column(name = "is_global_default", nullable = false)
  private boolean globalDefault = false;
}
