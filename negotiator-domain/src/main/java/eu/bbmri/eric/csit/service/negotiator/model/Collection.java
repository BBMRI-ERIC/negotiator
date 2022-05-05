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
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString.Exclude;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity(name = "Collection")
@Table(name = "collection")
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

  @NotNull
  @Column(name = "source_id")
  private String sourceId;

  @NotNull private String name;

  @Column(columnDefinition = "VARCHAR(512)")
  private String description;
}
