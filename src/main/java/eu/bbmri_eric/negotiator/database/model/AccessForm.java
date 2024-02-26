package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
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
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private SortedSet<AccessFormSectionLink> formLinks = new TreeSet<>();

  public AccessForm(String name) {
    this.name = name;
  }

  public Set<AccessFormSection> getSections() {
    return formLinks.stream()
        .map(AccessFormSectionLink::getAccessFormSection)
        .collect(Collectors.toUnmodifiableSet());
  }

  public void addSection(AccessFormSection section, int sectionOrder) {
    formLinks.add(new AccessFormSectionLink(this, section, sectionOrder));
  }

  public void linkElementToSection(
      AccessFormSection section, AccessFormElement element, int elementOrder, boolean isRequired) {
    Optional<AccessFormSectionLink> accessFormSectionLink =
        formLinks.stream().filter(link -> link.getAccessFormSection().equals(section)).findFirst();
    System.out.println(accessFormSectionLink.isPresent());
    System.out.println(accessFormSectionLink.get().getAccessFormSection().getName());
    accessFormSectionLink.ifPresent(
        formSectionLink ->
            formSectionLink.addElementLink(
                new AccessFormSectionElementLink(
                    formSectionLink, element, isRequired, elementOrder)));
    accessFormSectionLink
        .get()
        .getAccessFormSectionElementLinks()
        .forEach(link -> System.out.println(link.getAccessFormElement().getName()));
    formLinks.add(accessFormSectionLink.get());
    System.out.println("---");
    accessFormSectionLink
        .get()
        .getAccessFormSection()
        .getAccessFormElements()
        .forEach(element1 -> System.out.println(element1.getName()));
    System.out.println("---");
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
