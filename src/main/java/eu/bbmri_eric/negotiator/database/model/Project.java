package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.*;
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
@Table(name = "project")
@NamedEntityGraph(
    name = "project-detailed",
    attributeNodes = {@NamedAttributeNode("payload")})
public class Project extends AuditEntity {

  @JdbcTypeCode(SqlTypes.JSON)
  @NotNull
  private String payload;

  @Exclude
  @ManyToMany(mappedBy = "projects")
  private Set<Person> persons;
}
