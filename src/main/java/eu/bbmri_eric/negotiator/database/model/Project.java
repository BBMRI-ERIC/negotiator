package eu.bbmri_eric.negotiator.database.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "project")
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
@NamedEntityGraph(
    name = "project-detailed",
    attributeNodes = {@NamedAttributeNode("payload")})
public class Project extends AuditEntity {

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  @NotNull
  private String payload;

  //  @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
  //  @Exclude
  //  private Set<Negotiation> negotiations;

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
