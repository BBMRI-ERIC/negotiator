package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
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
@Table(name = "lifecycle_request_collection_status")
public class LifecycleRequestCollectionStatus extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
  @Exclude
  private Negotiation negotiation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resource_id")
  @Exclude
  private Resource resource;

  private String status;
  private String statusType;
  private Date statusDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "status_by")
  @Exclude
  private Person Person;

  @Lob // TODO: should this one be Json instead of txt blob?
  private String statusJson;
}
