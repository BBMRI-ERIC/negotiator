package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.Set;
import java.util.SortedSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
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
@SequenceGenerator(name = "access_form_id_seq", initialValue = 100)
public class AccessCriteriaSet extends AuditEntity {

  @Id
  @GeneratedValue(generator = "access_form_id_seq")
  @Column(name = "id")
  private Long id;

  private String name;

  @OneToMany(mappedBy = "accessCriteriaSet", fetch = FetchType.LAZY)
  @Exclude
  private Set<Resource> resources;

  @OneToMany(mappedBy = "accessCriteriaSet", fetch = FetchType.LAZY)
  @Exclude
  private SortedSet<AccessCriteriaSection> sections;
}
