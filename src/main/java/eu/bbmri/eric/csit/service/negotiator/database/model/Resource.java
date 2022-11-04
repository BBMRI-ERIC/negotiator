package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "Resource")
@Table(name = "resource")
@NamedEntityGraph(
    name = "resource-with-children",
    attributeNodes = {
        @NamedAttributeNode("sourceId"),
        @NamedAttributeNode("name"),
        @NamedAttributeNode("type"),
        @NamedAttributeNode("description"),
        @NamedAttributeNode("parent")
    })
public class Resource extends BaseEntity {
  @NotNull
  private String name;

  @Column(columnDefinition = "VARCHAR(5000)")
  private String description;

  @NotNull private String sourceId;

  @NotNull
  private String type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  @Exclude
  @Nullable
  private Resource parent;

  @OneToMany(mappedBy = "parent")
  private Set<Resource> children = new HashSet<>();

  @ManyToMany(mappedBy = "resources")
  @Exclude
  private Set<Person> persons = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @Exclude
  @JsonIgnore
  @NotNull
  private DataSource dataSource;
}
