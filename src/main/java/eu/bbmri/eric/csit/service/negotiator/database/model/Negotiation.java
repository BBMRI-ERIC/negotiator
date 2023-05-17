package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;

import java.util.*;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.json.JSONArray;
import org.json.JSONObject;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "negotiation")
@TypeDef(typeClass = JsonType.class, name = "json")
@NamedEntityGraph(
    name = "negotiation-with-detailed-children",
    attributeNodes = {
        @NamedAttributeNode(value = "persons", subgraph = "persons-with-roles"),
        @NamedAttributeNode(value = "requests", subgraph = "requests-detailed"),
    },
    subgraphs = {
        @NamedSubgraph(
            name = "persons-with-roles",
            attributeNodes = {
                @NamedAttributeNode(value = "person"),
                @NamedAttributeNode(value = "role")
            }),
        @NamedSubgraph(
            name = "requests-detailed",
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

  @OneToMany(
      mappedBy = "negotiation",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
      fetch = FetchType.LAZY)
  @Exclude
  private Set<PersonNegotiationRole> persons = new HashSet<>();

  @OneToMany(mappedBy = "negotiation", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  @Exclude
  private Set<Request> requests;

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private String payload;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Negotiation negotiation = (Negotiation) o;
    return Objects.equals(getId(), negotiation.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  public NegotiationResources getAllResources() {
    NegotiationResources negotiationResources = new NegotiationResources();
    Set<Resource> resources = new HashSet<>();
    for (Request request : requests) {
      resources.addAll(request.getResources());
    }
    negotiationResources.setNegotiationId(getId());
    negotiationResources.setResources(resources.stream().toList());
    return negotiationResources;
  }
}
