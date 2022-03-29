package eu.bbmri.eric.csit.service.model;

import java.util.Date;
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
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "project")
public class Project extends BaseEntity {

  @ManyToMany(mappedBy = "projects")
  Set<Person> persons;
  @ManyToMany(mappedBy = "projects")
  Set<Attachment> attachments;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private Person createdBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by", insertable = false, updatable = false)
  private Person modifiedBy;
  @NonNull
  private String title;
  @Lob
  private String projectDescription;
  @Lob
  private String ethicsVote;
  private Boolean testProject;
  private Date expectedEndDate;
  private Boolean expectedDataGeneration;
}
