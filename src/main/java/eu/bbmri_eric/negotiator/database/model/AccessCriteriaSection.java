package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Class representing an access form section that groups access form elements. */
@ToString
@Entity
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Getter
@Setter
@Builder
public class AccessCriteriaSection extends AuditEntity
    implements Comparable<AccessCriteriaSection> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @OneToMany(mappedBy = "accessCriteriaSection", fetch = FetchType.EAGER)
  @ToString.Exclude
  private Set<FormSectionLink> linkedForms = new HashSet<>();

  @OneToMany(mappedBy = "linkedSection", fetch = FetchType.LAZY)
  @ToString.Exclude
  private Set<AccessCriteria> allowedAccessCriteria = new HashSet<>();

  public AccessCriteriaSection(String name, String label, String description) {
    this.name = name;
    this.label = label;
    this.description = description;
  }

  public Set<AccessCriteria> getAccessCriteria() {
    return linkedForms.stream()
        .map(FormSectionLink::getSectionElementLinks)
        .map(Set::iterator)
        .map(Iterator::next)
        .map(SectionElementLink::getAccessCriteria)
        .collect(java.util.stream.Collectors.toSet());
  }


  @Override
  public int compareTo(AccessCriteriaSection section) {
    if (this.getId() < section.getId()) {
      return -1;
    } else if (this.getId().equals(section.getId())) {
      return 0;
    } else {
      return 1;
    }
  }
}
