package eu.bbmri_eric.negotiator.database.model;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.Objects;

/** Object depicting a requirement of filling out an access form in a particular Resource state. */
@Entity
public class InformationRequirement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne private AccessForm requiredAccessForm;

  @Enumerated(EnumType.STRING)
  private NegotiationResourceEvent forEvent;

  protected InformationRequirement() {}

  public InformationRequirement(
      Long id, AccessForm requiredAccessForm, NegotiationResourceEvent forEvent) {
    this.id = id;
    this.requiredAccessForm = requiredAccessForm;
    this.forEvent = forEvent;
  }

  public InformationRequirement(AccessForm requiredAccessForm, NegotiationResourceEvent forEvent) {
    this.requiredAccessForm = requiredAccessForm;
    this.forEvent = forEvent;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public AccessForm getRequiredAccessForm() {
    return requiredAccessForm;
  }

  public void setRequiredAccessForm(AccessForm requiredAccessForm) {
    this.requiredAccessForm = requiredAccessForm;
  }

  public NegotiationResourceEvent getForEvent() {
    return forEvent;
  }

  public void setForEvent(NegotiationResourceEvent forEvent) {
    this.forEvent = forEvent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InformationRequirement that = (InformationRequirement) o;
    return Objects.equals(id, that.id)
        && Objects.equals(requiredAccessForm, that.requiredAccessForm)
        && forEvent == that.forEvent;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, requiredAccessForm, forEvent);
  }
}
