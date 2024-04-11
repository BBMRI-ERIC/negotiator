package eu.bbmri_eric.negotiator.database.model;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class NegotiationEventMetadata {
  @Id @GeneratedValue private Long id;

  @Enumerated(EnumType.STRING)
  @Column(unique = true)
  private NegotiationEvent value;

  private String label;
  private String description;

  public NegotiationEventMetadata(NegotiationEvent value, String label, String description) {
    this.value = value;
    this.label = label;
    this.description = description;
  }
}
