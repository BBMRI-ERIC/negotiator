package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString.Exclude;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Class that adds fields util for Auditing purpose. It is intended to be extended by classes that
 * needs auditing information
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditEntity {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  @Column(name = "id")
  private String id;

  @CreatedDate
  @Exclude
  private LocalDateTime creationDate;

  @LastModifiedDate
  @Exclude
  private LocalDateTime modifiedDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", updatable = false)
  @CreatedBy
  @Exclude
  private Person createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by")
  @LastModifiedBy
  @Exclude
  private Person modifiedBy;
}
