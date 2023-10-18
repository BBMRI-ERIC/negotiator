package eu.bbmri.eric.csit.service.negotiator.database.model;

import javax.persistence.*;
import lombok.*;
import lombok.ToString.Exclude;

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
                @NamedSubgraph(name = "organization_details", attributeNodes = {
                        @NamedAttributeNode(value = "externalId")
                })
        }
)
public class Post extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
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
}
