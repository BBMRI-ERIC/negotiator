package eu.bbmri_eric.negotiator.info_submission;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

/** Represents a submission of additional information by the resource representative. */
@Setter
@Getter
@Entity
public class InformationSubmission {

  protected InformationSubmission() {}

  protected InformationSubmission(
      Long id,
      InformationRequirement requirement,
      Resource resource,
      Negotiation negotiation,
      String payload) {
    this.id = id;
    this.requirement = requirement;
    this.resource = resource;
    this.negotiation = negotiation;
    this.payload = payload;
  }

  public InformationSubmission(
      InformationRequirement requirement,
      Resource resource,
      Negotiation negotiation,
      String payload) {
    this.requirement = requirement;
    this.resource = resource;
    this.negotiation = negotiation;
    this.payload = payload;
  }

  public InformationSubmission(
      InformationRequirement requirement,
      Resource resource,
      Negotiation negotiation,
      String payload,
      boolean submitted) {
    this.requirement = requirement;
    this.resource = resource;
    this.negotiation = negotiation;
    this.payload = payload;
    this.submitted = submitted;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "requirement_id")
  private InformationRequirement requirement;

  @ManyToOne
  @JoinColumn(name = "resource_id")
  private Resource resource;

  @ManyToOne
  @JoinColumn(name = "negotiation_id")
  private Negotiation negotiation;

  @Type(JsonType.class)
  @Column(columnDefinition = "json")
  private String payload;

  private boolean submitted = false;
}
