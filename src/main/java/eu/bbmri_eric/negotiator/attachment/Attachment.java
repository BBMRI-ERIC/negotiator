package eu.bbmri_eric.negotiator.attachment;

import eu.bbmri_eric.negotiator.common.AuditEntity;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Types;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "attachment")
public class Attachment extends AuditEntity {

  @Id
  @GeneratedValue(generator = "uuid")
  @UuidGenerator
  @Column(name = "id")
  private String id;

  @ManyToOne
  @JoinColumn(name = "negotiation_id")
  @Exclude
  Negotiation negotiation;

  @ManyToOne
  @JoinColumn(name = "organization_id")
  @Exclude
  private Organization organization;

  private String name;

  @JdbcTypeCode(Types.VARBINARY)
  @Column(columnDefinition = "BYTEA")
  private byte[] payload;

  private Long size;

  private String contentType;

  public boolean isPublic() {
    return this.organization == null;
  }
}
