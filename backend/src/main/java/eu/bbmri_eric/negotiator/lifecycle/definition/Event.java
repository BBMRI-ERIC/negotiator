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

/**
 * A named trigger within one Definition Version. An Event may have no Transition, like the Override
 * Event.
 *
 * <p>Nothing on an Event is editable, because an Information Requirement finds its Event by name.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lifecycle_definition_id", nullable = false, updatable = false)
  private LifecycleDefinition lifecycleDefinition;

  @Column(nullable = false, updatable = false)
  private String name;
}
