package eu.bbmri_eric.negotiator.database.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;
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

  private String name;

  @Type(type = "org.hibernate.type.BinaryType")
  @Column(columnDefinition = "BYTEA")
  private byte[] payload;

  private Long size;

  private String contentType;
}
