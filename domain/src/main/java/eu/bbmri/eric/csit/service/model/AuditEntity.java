package eu.bbmri.eric.csit.service.model;

import java.util.Date;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.ToString.Exclude;

/**
 * Class that adds fields util for Auditing purpose. It is intended to be extended by classes that
 * needs auditing information
 */
public abstract class AuditEntity extends BaseEntity {
  @Exclude private Date creationDate;
  @Exclude private Date modifiedDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  @Exclude
  private Person createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by", insertable = false, updatable = false)
  @Exclude
  private Person modifiedBy;
}
