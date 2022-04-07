package eu.bbmri.eric.csit.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ser.Serializers.Base;
import com.sun.istack.NotNull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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

  //  @ManyToMany(mappedBy = "collections")
  //  @Exclude
  //  private Set<Person> persons = new HashSet<>();

  //  @ManyToMany(mappedBy = "collections")
  //  @Exclude
  //  private Set<Query> queries = new HashSet<>();

//  @Id @GeneratedValue private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @Exclude
  @JsonIgnore
  private DataSource dataSource;

  //  @ManyToOne(fetch = FetchType.LAZY)
  //  @JoinColumn(name = "biobank_id")
  //  @NotNull
  //  @Exclude
  //  private Biobank biobank;

  @NotNull
  @Column(name = "source_id")
  private String sourceId;

  @NotNull private String name;

  private String description;
}
