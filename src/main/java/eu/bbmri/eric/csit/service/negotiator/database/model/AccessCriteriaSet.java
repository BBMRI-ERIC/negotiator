package eu.bbmri.eric.csit.service.negotiator.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.Set;
import java.util.SortedSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@NamedEntityGraph(
    name = "access-criteria-set-with-details",
    attributeNodes = {
      @NamedAttributeNode(value = "name"),
      @NamedAttributeNode(value = "sections", subgraph = "access-criteria-sections"),
    },
    subgraphs = {
      @NamedSubgraph(
          name = "access-criteria-sections",
          attributeNodes = {
            @NamedAttributeNode(value = "name"),
            @NamedAttributeNode(value = "label"),
            @NamedAttributeNode(value = "description"),
            @NamedAttributeNode(
                value = "accessCriteriaSectionLink",
                subgraph = "access-criteria-section-with-details"),
          }),
      @NamedSubgraph(
          name = "access-criteria-section-with-details",
          attributeNodes = {
            @NamedAttributeNode(value = "accessCriteria", subgraph = "access-criteria-with-details")
          }),
      @NamedSubgraph(
          name = "access-criteria-with-details",
          attributeNodes = {
            @NamedAttributeNode(value = "name"),
            @NamedAttributeNode(value = "label"),
            @NamedAttributeNode(value = "description"),
            @NamedAttributeNode(value = "type")
          })
    })
public class AccessCriteriaSet extends BaseEntity {

  private String name;

  @OneToMany(mappedBy = "accessCriteriaSet", fetch = FetchType.LAZY)
  @Exclude
  private Set<Resource> resources;

  @OneToMany(mappedBy = "accessCriteriaSet", fetch = FetchType.LAZY)
  @OrderBy("id ASC")
  @Exclude
  private SortedSet<AccessCriteriaSection> sections;
}
