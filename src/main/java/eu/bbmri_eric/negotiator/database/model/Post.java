package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.annotations.UuidGenerator;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "post")
@NamedEntityGraph(
    name = "post-with-details",
    attributeNodes = {
      @NamedAttributeNode(value = "text"),
      @NamedAttributeNode(value = "type"),
      @NamedAttributeNode(value = "createdBy"),
      @NamedAttributeNode(value = "organization", subgraph = "organization_details")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "organization_details",
          attributeNodes = {@NamedAttributeNode(value = "externalId")})
    })
public class Post extends AuditEntity {

  @Id
  @GeneratedValue(generator = "uuid")
  @UuidGenerator
  @Column(name = "id")
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "negotiation_id")
  @Exclude
  private Negotiation negotiation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  @Exclude
  private Organization organization;

  @Column(columnDefinition = "TEXT")
  private String text;

  @Enumerated(EnumType.STRING)
  private PostStatus status;

  @Enumerated(EnumType.STRING)
  private PostType type;

  public boolean isPublic() {
    return this.type == PostType.PUBLIC;
  }
}
