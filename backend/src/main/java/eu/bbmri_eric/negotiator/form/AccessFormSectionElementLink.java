package eu.bbmri_eric.negotiator.form;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(name = "section_element_link_id_seq", allocationSize = 1)
class AccessFormSectionElementLink implements Comparable<AccessFormSectionElementLink> {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "section_element_link_id_seq")
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "access_form_section_link_id")
  @ToString.Exclude
  private AccessFormSectionLink accessFormSectionLink;

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
  @JoinColumn(name = "access_form_element_id")
  private AccessFormElement accessFormElement;

  private boolean isRequired;
  private int elementOrder;

  protected AccessFormSectionElementLink(
      AccessFormSectionLink accessFormSectionLink,
      AccessFormElement accessFormElement,
      boolean isRequired,
      int elementOrder) {
    if (Objects.nonNull(accessFormElement.getLinkedSection())
        && !accessFormElement
            .getLinkedSection()
            .equals(accessFormSectionLink.getAccessFormSection())) {
      throw new IllegalArgumentException(
          "Access form element is not allowed to be linked to this section and cannot be added.");
    }
    this.accessFormSectionLink = accessFormSectionLink;
    this.accessFormElement = accessFormElement;
    this.isRequired = isRequired;
    this.elementOrder = elementOrder;
  }

  @Override
  public int compareTo(AccessFormSectionElementLink o) {
    return Integer.compare(this.elementOrder, o.elementOrder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessFormSectionElementLink that = (AccessFormSectionElementLink) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
