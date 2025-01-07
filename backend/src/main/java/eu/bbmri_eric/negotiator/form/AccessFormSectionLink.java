package eu.bbmri_eric.negotiator.form;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.annotations.NaturalId;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@SequenceGenerator(name = "access_form_section_link_id_seq", initialValue = 100)
@CommonsLog
class AccessFormSectionLink implements Comparable<AccessFormSectionLink> {
  @Id
  @GeneratedValue(generator = "access_form_section_link_id_seq", strategy = GenerationType.SEQUENCE)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "access_form_id")
  @NaturalId
  @ToString.Exclude
  private AccessForm accessForm;

  @ManyToOne
  @JoinColumn(name = "access_form_section_id")
  @NaturalId
  private AccessFormSection accessFormSection;

  @OneToMany(
      mappedBy = "accessFormSectionLink",
      cascade = {CascadeType.PERSIST},
      orphanRemoval = true)
  private SortedSet<AccessFormSectionElementLink> accessFormSectionElementLinks = new TreeSet<>();

  private int sectionOrder;

  public AccessFormSectionLink(
      AccessForm accessForm, AccessFormSection accessFormSection, int sectionOrder) {
    this.accessForm = accessForm;
    this.accessFormSection = accessFormSection;
    this.sectionOrder = sectionOrder;
  }

  public void addElementLink(AccessFormSectionElementLink elementLink) {
    this.accessFormSectionElementLinks.add(elementLink);
    this.getAccessFormSection().addLinkedForm(this);
  }

  @Override
  public int compareTo(AccessFormSectionLink o) {
    return Integer.compare(this.sectionOrder, o.sectionOrder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessFormSectionLink that = (AccessFormSectionLink) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
