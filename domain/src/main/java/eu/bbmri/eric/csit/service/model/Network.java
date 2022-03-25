package eu.bbmri.eric.csit.service.model;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "network")
public class Network extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private Person createdBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by", insertable = false, updatable = false)
  private Person modifiedBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  private Datasource datasource;
  private String sourceId;
  private String name;
  private String acronym;
  @Lob
  private String description;
  @ManyToMany
  @JoinTable(
      name = "network_biobank_link",
      joinColumns = @JoinColumn(name = "biobank_id"),
      inverseJoinColumns = @JoinColumn(name = "network_id"))
  Set<Biobank> biobanks ;
  @ManyToMany
  @JoinTable(
      name = "network_collection_link",
      joinColumns = @JoinColumn(name = "collection_id"),
      inverseJoinColumns = @JoinColumn(name = "network_id"))
  Set<Collection> collections;
  @ManyToMany(mappedBy = "networks")
  Set<Person> persons;

}
