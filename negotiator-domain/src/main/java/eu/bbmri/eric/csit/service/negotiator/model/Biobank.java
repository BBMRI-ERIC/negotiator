package eu.bbmri.eric.csit.service.negotiator.model;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
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
@Entity(name = "Biobank")
@Table(name = "biobank")
public class Biobank extends BaseEntity {

  @ManyToMany(mappedBy = "biobanks")
  @Exclude
  private Set<Person> persons = new HashSet<>();
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

  @NotNull private String name;

  @NotNull private String sourceId;

  @Column(columnDefinition = "VARCHAR(5000)")
  private String description;
}
