package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "negotiation_resource_status")
public class NegotiationResourceStatus extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "negotiation_id")
  @Exclude
  private Negotiation negotiation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resource_id")
  @Exclude
  private Resource resource;

  private String status;
  private Date statusDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "status_by")
  @Exclude
  private Person Person;

}
