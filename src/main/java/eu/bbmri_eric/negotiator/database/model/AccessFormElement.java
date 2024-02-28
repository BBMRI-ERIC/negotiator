package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
@SequenceGenerator(name = "access_form_element_id_seq", initialValue = 100)
public class AccessFormElement extends AuditEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "access_form_element_id_seq")
  private Long id;

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @NotNull private String type;

  @OneToMany(mappedBy = "accessFormElement")
  @ToString.Exclude
  private Set<AccessFormSectionElementLink> linkedSections;

  @Transient private boolean isRequired;


  @ManyToOne
  @JoinColumn(name = "access_form_section_id")
  @ToString.Exclude
  private AccessFormSection linkedSection;

  public AccessFormElement(String name, String label, String description, String type) {
    this.name = name;
    this.label = label;
    this.description = description;
    this.type = type;
  }

  public boolean isRequired() {
    return isRequired;
  }

  void setRequired(boolean required) {
    isRequired = required;
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessFormElement that = (AccessFormElement) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
