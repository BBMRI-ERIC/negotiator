package eu.bbmri.eric.csit.service.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
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

  @NotNull private String title;

  @Column(columnDefinition = "VARCHAR(512)")
  @NotNull
  private String description;

  @Column(columnDefinition = "VARCHAR(512)")
  private String ethicsVote;

  private Boolean isTestProject;

  private LocalDate expectedEndDate;

  private Boolean expectedDataGeneration;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Project project = (Project) o;
    return Objects.equals(getTitle(), project.getTitle())
        && Objects.equals(getDescription(), project.getDescription())
        && Objects.equals(getEthicsVote(), project.getEthicsVote())
        && Objects.equals(getIsTestProject(), project.getIsTestProject())
        && Objects.equals(getExpectedEndDate(), project.getExpectedEndDate())
        && Objects.equals(getExpectedDataGeneration(), project.getExpectedDataGeneration());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getPersons(),
        getAttachments(),
        getCreatedBy(),
        getModifiedBy(),
        getTitle(),
        getDescription(),
        getEthicsVote(),
        getIsTestProject(),
        getExpectedEndDate(),
        getExpectedDataGeneration());
  }
}
