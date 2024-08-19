package eu.bbmri_eric.negotiator.database.model;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  @JdbcTypeCode(SqlTypes.JSON)
  private String payload;
}
