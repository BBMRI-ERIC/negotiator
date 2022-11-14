package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
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
        @NamedAttributeNode(value = "accessCriteriaSetLink", subgraph = "access-criteria-set"),
    },
    subgraphs = {
        @NamedSubgraph(
            name = "access-criteria-set",
            attributeNodes = {
                @NamedAttributeNode(value = "accessCriteria", subgraph = "access-criteria-with-details"),
            }),
        @NamedSubgraph(
            name = "access-criteria-with-details",
            attributeNodes = {
                @NamedAttributeNode(value = "name"),
                @NamedAttributeNode(value = "description"),
                @NamedAttributeNode(value = "type")
            })
    }
)
public class AccessCriteriaSet extends BaseEntity {

  @OneToMany(mappedBy = "accessCriteriaSet", fetch = FetchType.LAZY)
  @Exclude
  private Set<Resource> resources;

  @OneToMany(mappedBy = "accessCriteriaSet")
  @OrderBy("ordering ASC")
  @Exclude
  private List<AccessCriteriaSetLink> accessCriteriaSetLink;

}
