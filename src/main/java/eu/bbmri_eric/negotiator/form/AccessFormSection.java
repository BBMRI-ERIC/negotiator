package eu.bbmri_eric.negotiator.form;

import eu.bbmri_eric.negotiator.negotiation.AuditEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.springframework.lang.Nullable;

/** Class representing an access form section that groups access form elements. */
@ToString
@Entity
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
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

  @OneToMany(mappedBy = "linkedSection")
  @ToString.Exclude
  private Set<AccessFormElement> allowedElements = new HashSet<>();

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

  /**
   * Get the access form elements linked to this section in this form.
   *
   * @return an unmodifiable set of access form elements.
   */
  public Set<AccessFormElement> getAccessFormElements() {
    AccessFormSectionLink linkedForm = getAccessFormLink();
    if (linkedForm == null) {
      return Set.of();
    }
    return linkedElements(linkedForm);
  }

  @NonNull
  private static Set<AccessFormElement> linkedElements(AccessFormSectionLink accessFormLink) {
    SortedSet<AccessFormSectionElementLink> sectionElementLinks =
        accessFormLink.getAccessFormSectionElementLinks();
    return Collections.unmodifiableSet(getAccessFormElements(sectionElementLinks));
  }

  @NonNull
  private static Set<AccessFormElement> getAccessFormElements(
      SortedSet<AccessFormSectionElementLink> sectionElementLinks) {
    Set<AccessFormElement> elements = new LinkedHashSet<>();
    for (AccessFormSectionElementLink link : sectionElementLinks) {
      if (link.getAccessFormElement() == null) {
        continue;
      }
      AccessFormElement element = link.getAccessFormElement();
      element.setRequired(link.isRequired());
      elements.add(element);
    }
    return elements;
  }

  @Nullable
  private AccessFormSectionLink getAccessFormLink() {
    return linkedForms.stream()
        .filter(
            accessFormSectionLink -> accessFormSectionLink.getAccessForm().equals(this.accessForm))
        .findFirst()
        .orElse(null);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
