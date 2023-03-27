package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "negotiation")
//@EntityListeners(AuditingEntityListener.class)
@NamedEntityGraph(
    name = "negotiation-with-detailed-children",
    attributeNodes = {
        @NamedAttributeNode("project"),
        @NamedAttributeNode(value = "persons", subgraph = "persons-with-roles"),
        @NamedAttributeNode(value = "queries", subgraph = "queries-detailed"),
    },
    subgraphs = {
        @NamedSubgraph(
            name = "persons-with-roles",
            attributeNodes = {
                @NamedAttributeNode(value = "person"),
                @NamedAttributeNode(value = "role")
            }),
        @NamedSubgraph(
            name = "queries-detailed",
            attributeNodes = {
                @NamedAttributeNode(value = "resources", subgraph = "resources-with-parent")
            }),
        @NamedSubgraph(
            name = "resources-with-parent",
            attributeNodes = {@NamedAttributeNode("parent")})
    })
public class Negotiation extends AuditEntity {

  @ManyToMany(mappedBy = "negotiations")
  @Exclude
  Set<Attachment> attachments;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
  @JoinColumn(name = "project_id")
  @Exclude
  private Project project;

  @OneToMany(
      mappedBy = "negotiation",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
      fetch = FetchType.LAZY)
  @Exclude
  private Set<PersonNegotiationRole> persons = new HashSet<>();

  @OneToMany(mappedBy = "negotiation", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  @Exclude
  private Set<Request> queries;

  private String title;

  private String description;

  private Boolean isTest;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Negotiation negotiation = (Negotiation) o;
    return Objects.equals(getId(), negotiation.getId())
        && Objects.equals(getTitle(), negotiation.getTitle())
        && Objects.equals(getDescription(), negotiation.getDescription())
        && Objects.equals(getIsTest(), negotiation.getIsTest());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getTitle(), getDescription(), getIsTest());
  }
}
