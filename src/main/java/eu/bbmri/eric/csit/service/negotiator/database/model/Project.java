package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Convert(converter = JsonType.class, attributeName = "json")
@Table(name = "project")
@NamedEntityGraph(
    name = "project-detailed",
    attributeNodes = {@NamedAttributeNode("payload")})
public class Project extends AuditEntity {

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  @NotNull
  private String payload;

  @Exclude
  @ManyToMany(mappedBy = "projects")
  private Set<Person> persons;

  //  @Override
  //  public boolean equals(Object o) {
  //    if (this == o) {
  //      return true;
  //    }
  //    if (o == null || getClass() != o.getClass()) {
  //      return false;
  //    }
  //    Project project = (Project) o;
  //    return Objects.equals(getTitle(), project.getTitle())
  //        && Objects.equals(getDescription(), project.getDescription())
  //        && Objects.equals(getEthicsVote(), project.getEthicsVote())
  //        && Objects.equals(getIsTestProject(), project.getIsTestProject())
  //        && Objects.equals(getExpectedEndDate(), project.getExpectedEndDate())
  //        && Objects.equals(getExpectedDataGeneration(), project.getExpectedDataGeneration());
  //  }
  //
  //  @Override
  //  public int hashCode() {
  //    return Objects.hash(
  //        getTitle(),
  //        getDescription(),
  //        getEthicsVote(),
  //        getIsTestProject(),
  //        getExpectedEndDate(),
  //        getExpectedDataGeneration());
  //  }
}
