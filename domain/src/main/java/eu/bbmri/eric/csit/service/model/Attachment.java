package eu.bbmri.eric.csit.service.model;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
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
@Table(name = "attachment")
public class Attachment extends BaseEntity {

  @ManyToMany
  @JoinTable(
      name = "attachment_post_link",
      joinColumns = @JoinColumn(name = "post_id"),
      inverseJoinColumns = @JoinColumn(name = "attachment_id"))
  Set<Post> posts;
  @ManyToMany
  @JoinTable(
      name = "attachment_project_link",
      joinColumns = @JoinColumn(name = "project_id"),
      inverseJoinColumns = @JoinColumn(name = "attachment_id"))
  Set<Project> projects;
  @ManyToMany
  @JoinTable(
      name = "attachment_request_link",
      joinColumns = @JoinColumn(name = "request_id"),
      inverseJoinColumns = @JoinColumn(name = "attachment_id"))
  Set<Request> requests;
  @ManyToMany
  @JoinTable(
      name = "attachment_private_post_link",
      joinColumns = @JoinColumn(name = "post_id"),
      inverseJoinColumns = @JoinColumn(name = "attachment_id"))
  Set<Post> Posts;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private Person createdBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by", insertable = false, updatable = false)
  private Person modifiedBy;
  private String fileName;
  private String fileHash;
  private String fileSize;
  private String fileExtension;
  private String attachmentScope;

}
