package eu.bbmri.eric.csit.service.negotiator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.istack.NotNull;
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
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString.Exclude;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "Collection")
@Table(name = "collection")
@NamedEntityGraph(
    name = "collection-with-biobank",
    attributeNodes = {
      @NamedAttributeNode("sourceId"),
      @NamedAttributeNode("name"),
      @NamedAttributeNode("description"),
      @NamedAttributeNode("biobank")
    })
public class Collection extends BaseEntity {

  //  @ManyToMany(mappedBy = "collections")
  //  @Exclude
  //  private Set<Network> networks = new HashSet<>();

  @ManyToMany(mappedBy = "collections")
  @Exclude
  private Set<Person> persons = new HashSet<>();

  //  @ManyToMany(mappedBy = "collections")
  //  @Exclude
  //  private Set<Query> queries = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @Exclude
  @JsonIgnore
  private DataSource dataSource;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "biobank_id")
  @NotNull
  @Exclude
  @JsonIgnore
  private Biobank biobank;

  @NotNull private String sourceId;

  @NotNull private String name;

  @Column(columnDefinition = "VARCHAR(5000)")
  private String description;
}
