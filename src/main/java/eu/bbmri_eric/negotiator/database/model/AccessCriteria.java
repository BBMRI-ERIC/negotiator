package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Class representing an access form element. */
@ToString
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Builder
public class AccessCriteria extends AuditEntity implements Comparable<AccessCriteria> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @NotNull private String type;

  @OneToMany(mappedBy = "accessCriteria")
  @ToString.Exclude
  private Set<SectionElementLink> linkedSections;

  @ManyToOne
  @JoinColumn(name = "access_criteria_section_id")
  private AccessCriteriaSection linkedSection;

  public AccessCriteria(String name, String label, String description, String type) {
    this.name = name;
    this.label = label;
    this.description = description;
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessCriteria that = (AccessCriteria) o;
    return Objects.equals(id, that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(label, that.label)
        && Objects.equals(description, that.description)
        && Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, label, description, type);
  }

  @Override
  public int compareTo(AccessCriteria section) {
    if (this.getId() < section.getId()) {
      return -1;
    } else if (this.getId().equals(section.getId())) {
      return 0;
    } else {
      return 1;
    }
  }
}
