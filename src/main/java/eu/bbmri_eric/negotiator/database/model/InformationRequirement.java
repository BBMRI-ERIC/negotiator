package eu.bbmri_eric.negotiator.database.model;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
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
  private NegotiationResourceState forResourceState;

  protected InformationRequirement() {}

  public InformationRequirement(
      Long id, AccessForm requiredAccessForm, NegotiationResourceState forResourceState) {
    this.id = id;
    this.requiredAccessForm = requiredAccessForm;
    this.forResourceState = forResourceState;
  }

  public InformationRequirement(
      AccessForm requiredAccessForm, NegotiationResourceState forResourceState) {
    this.requiredAccessForm = requiredAccessForm;
    this.forResourceState = forResourceState;
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

  public NegotiationResourceState getForResourceState() {
    return forResourceState;
  }

  public void setForResourceState(NegotiationResourceState forResourceState) {
    this.forResourceState = forResourceState;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InformationRequirement that)) return false;

    return Objects.equals(id, that.id)
        && Objects.equals(requiredAccessForm, that.requiredAccessForm)
        && forResourceState == that.forResourceState;
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(requiredAccessForm);
    result = 31 * result + Objects.hashCode(forResourceState);
    return result;
  }
}
