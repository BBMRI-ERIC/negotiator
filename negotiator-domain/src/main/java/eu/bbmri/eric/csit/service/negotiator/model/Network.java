package eu.bbmri.eric.csit.service.negotiator.model;

import com.sun.istack.NotNull;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
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
@Entity(name = "Network")
@Table(name = "network")
public class Network extends BaseEntity {

  @ManyToMany
  @JoinTable(
      name = "network_biobank_link",
      joinColumns = {@JoinColumn(name = "biobank_id")},
      inverseJoinColumns = {@JoinColumn(name = "network_id")})
  @Exclude
  Set<Biobank> biobanks = new HashSet<>();

  @ManyToMany
  @JoinTable(
      name = "network_collection_link",
      joinColumns = {@JoinColumn(name = "collection_id")},
      inverseJoinColumns = {@JoinColumn(name = "network_id")})
  @Exclude
  Set<Collection> collections = new HashSet<>();

  @ManyToMany(mappedBy = "networks")
  @Exclude
  private Set<Person> persons;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @Exclude
  private DataSource dataSource;

  private String name;

  private String acronym;

  @NotNull
  private String sourceId;

  @Lob
  @Column(columnDefinition = "VARCHAR(512)")
  private String description;
}
