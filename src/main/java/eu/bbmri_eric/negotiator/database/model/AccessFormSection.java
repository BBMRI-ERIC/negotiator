package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
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
@SequenceGenerator(name = "access_form_section_id_seq", initialValue = 100)
public class AccessFormSection extends AuditEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "access_form_section_id_seq")
  private Long id;

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @OneToMany(mappedBy = "accessFormSection", fetch = FetchType.EAGER)
  @ToString.Exclude
  private Set<AccessFormSectionLink> linkedForms = new HashSet<>();

  @OneToMany(mappedBy = "linkedSection", fetch = FetchType.LAZY)
  @ToString.Exclude
  private Set<AccessFormElement> allowedAccessCriteria = new HashSet<>();

  @Transient private AccessForm accessForm;

  public AccessFormSection(String name, String label, String description) {
    this.name = name;
    this.label = label;
    this.description = description;
  }

  void setAccessForm(AccessForm accessForm) {
    this.accessForm = accessForm;
  }

  void addLinkedForm(AccessFormSectionLink linkedForm) {
    linkedForms.add(linkedForm);
  }

  public Set<AccessFormElement> getAccessFormElements() {
    Set<AccessFormElement> accessCriteria = new LinkedHashSet<>();
    AccessFormSectionLink linkedForm =
        linkedForms.stream()
            .filter(
                accessFormSectionLink ->
                    accessFormSectionLink.getAccessForm().equals(this.accessForm))
            .findFirst()
            .orElse(null);
    if (linkedForm == null) {
      return accessCriteria;
    }
    SortedSet<AccessFormSectionElementLink> sectionElementLinks =
        linkedForm.getAccessFormSectionElementLinks();
    for (AccessFormSectionElementLink link : sectionElementLinks) {
      if (link.getAccessFormElement() == null) {
        continue;
      }
      AccessFormElement element = link.getAccessFormElement();
      element.setRequired(link.isRequired());
      accessCriteria.add(element);
    }
    return accessCriteria;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessFormSection that = (AccessFormSection) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
