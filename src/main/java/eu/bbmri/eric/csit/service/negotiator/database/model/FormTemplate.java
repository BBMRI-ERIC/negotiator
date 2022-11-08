package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
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
@Table(name = "form_template")
@NamedEntityGraph(
    name = "template-with-details",
    attributeNodes = {
        @NamedAttributeNode("resource"),
        @NamedAttributeNode(value = "fields", subgraph = "fields-templates"),
    },
    subgraphs = {
        @NamedSubgraph(
            name = "field-templates",
            attributeNodes = {
                @NamedAttributeNode(value = "field", subgraph = "fields-with-details"),
//                @NamedAttributeNode(value = "label")
            }),
        @NamedSubgraph(
            name = "fields-with-details",
            attributeNodes = {
                @NamedAttributeNode(value = "name"),
                @NamedAttributeNode(value = "label")
            })
      }
)
public class FormTemplate extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resource_id")
  @Exclude
  private Resource resource;

  @Exclude
  @OneToMany(mappedBy = "template")
  private Set<FormFieldTemplateLink> fields;
}
