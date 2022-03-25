package eu.bbmri.eric.csit.service.model;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
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
@Table(name = "collection")
public class Collection extends BaseEntity {

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
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "biobank_id")
  private Biobank biobank;
  private String name;
  @Lob
  private String description;
  @ManyToMany(mappedBy = "collections")
  Set<Network> networks;
  @ManyToMany(mappedBy = "collections")
  Set<Person> persons;
  @ManyToMany(mappedBy = "collections")
  Set<Query> queries;


}
