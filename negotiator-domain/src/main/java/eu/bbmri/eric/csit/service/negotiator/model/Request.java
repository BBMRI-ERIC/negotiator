package eu.bbmri.eric.csit.service.negotiator.model;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
@Table(name = "request")
public class Request extends AuditEntity {

  @ManyToMany(mappedBy = "requests")
  @Exclude
  Set<Attachment> attachments;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  @Exclude
  private Project project;

  @OneToMany(mappedBy = "request")
  private Set<Query> queries;

  private String title;

  private String description;

  private Boolean isTest;

  private String token = UUID.randomUUID().toString().replace("-", "");

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Request request = (Request) o;
    return Objects.equals(getId(), request.getId())
        && Objects.equals(getTitle(), request.getTitle())
        && Objects.equals(getDescription(), request.getDescription())
        && Objects.equals(getIsTest(), request.getIsTest())
        && Objects.equals(getToken(), request.getToken());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(),
        getTitle(),
        getDescription(),
        getIsTest(),
        getToken());
  }
}
