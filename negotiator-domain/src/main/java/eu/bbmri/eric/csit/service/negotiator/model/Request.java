package eu.bbmri.eric.csit.service.negotiator.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "request")
@EntityListeners(AuditingEntityListener.class)
@NamedEntityGraph(
    name = "request-with-detailed-children",
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
            @NamedAttributeNode(value = "collections", subgraph = "collections-with-biobank")
          }),
      @NamedSubgraph(
          name = "collections-with-biobank",
          attributeNodes = {@NamedAttributeNode("biobank")})
    })
public class Request extends AuditEntity {

  @ManyToMany(mappedBy = "requests")
  @Exclude
  Set<Attachment> attachments;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
  @JoinColumn(name = "project_id")
  @Exclude
  private Project project;

  @OneToMany(
      mappedBy = "request",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
      fetch = FetchType.LAZY)
  @Exclude
  private Set<PersonRequestRole> persons = new HashSet<>();

  @OneToMany(mappedBy = "request", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  @Exclude
  private Set<Query> queries;

  private String title;

  private String description;

  private Boolean isTest;

  private String token = UUID.randomUUID().toString().replace("-", "");

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Request request = (Request) o;
    return Objects.equals(getId(), request.getId())
        && Objects.equals(getTitle(), request.getTitle())
        && Objects.equals(getDescription(), request.getDescription())
        && Objects.equals(getIsTest(), request.getIsTest())
        && Objects.equals(getToken(), request.getToken());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getTitle(), getDescription(), getIsTest(), getToken());
  }
}
