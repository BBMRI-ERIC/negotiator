package eu.bbmri_eric.negotiator.lifecycle.definition;

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
 * One immutable Definition Version: a complete graph of States, Events and Transitions, identified
 * by its row id alone.
 *
 * <p>{@code familyKey} is immutable and shared by every version of a Definition Family; {@code
 * version} is a per-family display integer with no identity role, so gaps in it are harmless.
 * {@code name} is a freely editable display label. Renaming a family therefore breaks nothing.
 *
 * <p>{@code globalDefault} is a fact about the <em>family</em>, and there is no family table to
 * hold it, so it is carried by each version row and travels with the family across its versions.
 *
 * <p>Only {@code name}, {@code active} and {@code globalDefault} are editable. The invariants this
 * row cannot express, and why they are left to publish-time validation instead, are recorded with
 * the DDL in {@code V36.0__add_lifecycle_definition_table.sql}.
 *
 * <p>Deliberately not an {@code AuditEntity}: definitions are immutable configuration and ADR 0003
 * places no audit requirement on them.
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

  @Builder.Default
  @Column(name = "is_global_default", nullable = false)
  private boolean globalDefault = false;
}
