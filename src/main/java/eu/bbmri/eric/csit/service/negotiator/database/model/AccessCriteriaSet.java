package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
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
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.collection.internal.PersistentSortedSet;

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
                @NamedAttributeNode(value = "title"),
                @NamedAttributeNode(value = "description"),
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

  private String name;

  @OneToMany(mappedBy = "accessCriteriaSet", fetch = FetchType.LAZY)
  @Exclude
  private Set<Resource> resources;

  @OneToMany(mappedBy = "accessCriteriaSet", fetch = FetchType.LAZY)
  @OrderBy("id ASC")
//  @Sort(type = SortType.NATURAL)
  @Exclude
  private SortedSet<AccessCriteriaSection> sections;

}
