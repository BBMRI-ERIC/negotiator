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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "project")
public class Project extends AuditEntity {

  @ManyToMany(mappedBy = "projects")
  @Exclude
  Set<Person> persons;

  @ManyToMany(mappedBy = "projects")
  @Exclude
  Set<Attachment> attachments;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  @Exclude
  private Person createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by", insertable = false, updatable = false)
  @Exclude
  private Person modifiedBy;

  @NonNull private String title;
  @Lob private String projectDescription;
  @Lob private String ethicsVote;
  private Boolean testProject;
  private Date expectedEndDate;
  private Boolean expectedDataGeneration;
}
