package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
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

  public AccessFormSection(String name, String label, String description) {
    this.name = name;
    this.label = label;
    this.description = description;
  }

  void addLinkedForm(AccessFormSectionLink linkedForm) {
    linkedForms.add(linkedForm);
  }

  public Set<AccessFormElement> getAccessFormElements() {
    Set<AccessFormElement> accessCriteria = new HashSet<>();
    for (AccessFormSectionLink linkedForm : linkedForms) {
      Set<AccessFormSectionElementLink> sectionElementLinks =
          linkedForm.getAccessFormSectionElementLinks();
      for (AccessFormSectionElementLink link : sectionElementLinks) {
        System.out.println(link.getAccessFormElement().getName());
      }
      Iterator<AccessFormSectionElementLink> iterator = sectionElementLinks.iterator();
      if (iterator.hasNext()) {
        AccessFormSectionElementLink link = iterator.next();
        AccessFormElement element = link.getAccessFormElement();
        element.setRequired(link.isRequired());
        element.setElementOrder(link.getElementOrder());
        accessCriteria.add(element);
      }
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
