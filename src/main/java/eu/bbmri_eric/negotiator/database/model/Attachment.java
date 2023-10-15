package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
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

  @JdbcTypeCode(Types.VARBINARY)
  @Column(columnDefinition = "BYTEA")
  private byte[] payload;

  private Long size;

  private String contentType;
}
