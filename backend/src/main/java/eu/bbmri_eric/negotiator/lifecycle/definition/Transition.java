package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
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
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One edge of a Definition Version's graph. Firing {@code event} in {@code fromState} moves to
 * {@code toState} if the caller holds any one of {@code requiredAuthority} and the Guards allow it.
 *
 * <p>The four references are immutable, so changing one makes a new Transition. Only {@code
 * requiredAuthority} is editable.
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
   * Stored rather than derived through {@code fromState}, because the loader reads a whole graph by
   * its Definition Version id. The database enforces that it agrees with the three references
   * below.
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

  /**
   * Never null. An empty set admits any person. Holding any one value is enough, and {@code SYSTEM}
   * stands alone. It is a {@code VARCHAR} array rather than a join table because nothing queries it
   * by element, and the array keeps the CHECK on one table.
   */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(nullable = false)
  private Set<RequiredAuthority> requiredAuthority;
}
