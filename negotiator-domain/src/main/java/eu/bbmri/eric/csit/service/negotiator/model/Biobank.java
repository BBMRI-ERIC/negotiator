package eu.bbmri.eric.csit.service.negotiator.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity(name = "Biobank")
@Table(name = "biobank")
public class Biobank extends BaseEntity {

  //  @ManyToMany(mappedBy = "biobanks")
  //  @Exclude
  //  private Set<Person> persons = new HashSet<>();
  //
  //  @ManyToMany(mappedBy = "biobanks")
  //  @Exclude
  //  private Set<Network> networks = new HashSet<>();

  //  @ManyToMany(mappedBy = "biobanks")
  //  @Exclude
  //  private Set<Query> queries = new HashSet<>();

  //  @ManyToOne(fetch = FetchType.LAZY)
  //  @JoinColumn(name = "data_source_id")
  //  @Exclude
  //  private DataSource dataSource;

  private String name;

  private String sourceId;

  @Column(columnDefinition = "VARCHAR(512)")
  private String description;
}
