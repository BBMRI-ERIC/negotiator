package eu.bbmri.eric.csit.service.model;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
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
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "biobank")
public class Biobank extends BaseEntity {

  @ManyToMany(mappedBy = "biobanks")
  @Exclude
  private Set<Person> persons = new HashSet<>();

  @ManyToMany(mappedBy = "biobanks")
  @Exclude
  private Set<Network> networks = new HashSet<>();

  @ManyToMany(mappedBy = "biobanks")
  @Exclude
  private Set<Query> queries = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @Exclude
  private DataSource dataSource;

  private String sourceId;

  private String name;

  @Lob
  @Column(columnDefinition = "VARCHAR(512)")
  private String description;
}
