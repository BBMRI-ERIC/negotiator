package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString.Exclude;

/** Class representing an access form. */
@Entity
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Getter
@Setter
@SequenceGenerator(name = "access_form_id_seq", initialValue = 100)
public class AccessForm extends AuditEntity {

  @Id
  @GeneratedValue(generator = "access_form_id_seq")
  @Column(name = "id")
  private Long id;

  private String name;

  @OneToMany(mappedBy = "accessForm", fetch = FetchType.LAZY)
  @Exclude
  private Set<Resource> resources;

  @OneToMany(
      mappedBy = "accessForm",
      fetch = FetchType.EAGER,
      cascade = CascadeType.PERSIST,
      orphanRemoval = true)
  private SortedSet<AccessFormSectionLink> formLinks = new TreeSet<>();

  public AccessForm(String name) {
    this.name = name;
  }

  /**
   * Get access form sections that are linked to this particular form. These sections then contain
   * individual elements.
   *
   * @return an unmodifiable set of linked sections.
   */
  public Set<AccessFormSection> getLinkedSections() {
    Set<AccessFormSection> linkedSections = new LinkedHashSet<>();
    for (AccessFormSectionLink link : formLinks) {
      link.getAccessFormSection().setAccessForm(this);
      linkedSections.add(link.getAccessFormSection());
    }
    return linkedSections;
  }

  /**
   * Link a section to the form.
   *
   * @param section that should be linked.
   * @param sectionOrder the order of the section.
   */
  public void linkSection(AccessFormSection section, int sectionOrder) {
    formLinks.add(new AccessFormSectionLink(this, section, sectionOrder));
  }

  /**
   * Link an element to a section.
   *
   * @param section a section of the form to which the element should be linked.
   * @param element an element that should be linked.
   * @param elementOrder the order of the element.
   * @param isRequired whether the element is marked as required.
   */
  public void linkElementToSection(
      AccessFormSection section, AccessFormElement element, int elementOrder, boolean isRequired) {
    AccessFormSectionLink accessFormSectionLink =
        formLinks.stream()
            .filter(link -> link.getAccessFormSection().equals(section))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Section not found"));
    accessFormSectionLink.addElementLink(
        new AccessFormSectionElementLink(accessFormSectionLink, element, isRequired, elementOrder));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessForm that = (AccessForm) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
