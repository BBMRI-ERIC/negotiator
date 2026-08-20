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
 * The Wiring that attaches an Action to the Transition it runs after. It has no Definition Version
 * reference because the Transition implies one. Guards run before a commit and Actions after, so
 * the two never share an ordering.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
class ActionWiring {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The Transition this Action runs after. Actions are never definition-scoped. */
  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transition_id", nullable = false, updatable = false)
  private Transition transition;

  /** Names a Java strategy from a fixed catalogue; changing it makes a different wiring. */
  @Setter(AccessLevel.NONE)
  @Column(nullable = false, updatable = false)
  private String typeKey;

  /** Per-strategy configuration. Null is legal: a strategy that takes no parameters needs none. */
  @JdbcTypeCode(SqlTypes.JSON)
  private String params;

  @Column(nullable = false)
  private Integer sortOrder;
}
