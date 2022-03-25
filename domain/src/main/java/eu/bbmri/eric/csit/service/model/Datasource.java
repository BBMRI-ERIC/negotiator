package eu.bbmri.eric.csit.service.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
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
@Table(name = "data_source")
public class Datasource extends BaseEntity {

  // private Date creationDate;
  // private Date modifiedDate;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private Person createdBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by", insertable = false, updatable = false)
  private Person modifiedBy;
  private String name;
  private String description;
  private String URL;
  private String apiURL;
  private String apiUsername;
  private String apiPassword;
  private String apiType;
  private String resourceNetwork;
  private String resourceBiobank;
  private String resourceCollection;
  private Boolean syncActive;
  private Boolean sourcePrefix;


}
