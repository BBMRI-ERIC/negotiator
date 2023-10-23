package eu.bbmri.eric.csit.service.negotiator.database.model;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.annotations.Type;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "attachment")
@NamedEntityGraph(
    name = "attachment-metadata",
    attributeNodes = {
      @NamedAttributeNode(value = "id"),
      @NamedAttributeNode(value = "name"),
      @NamedAttributeNode(value = "size"),
      @NamedAttributeNode(value = "contentType"),
    })
public class Attachment extends AuditEntity {

  @ManyToOne
  @JoinColumn(name = "negotiation_id")
  @Exclude
  Negotiation negotiation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  @Exclude
  private Organization organization;

  private String name;

  @Type(type = "org.hibernate.type.BinaryType")
  @Column(columnDefinition = "BYTEA")
  private byte[] payload;

  private Long size;

  private String contentType;
}
