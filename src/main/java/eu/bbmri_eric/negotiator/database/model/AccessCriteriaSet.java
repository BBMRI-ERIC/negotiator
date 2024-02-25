package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

/** Class representing an access form. */
@ToString
@Entity
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Getter
@Builder
@Setter
@SequenceGenerator(name = "access_form_id_seq", initialValue = 100)
public class AccessCriteriaSet extends AuditEntity {

  @Id
  @GeneratedValue(generator = "access_form_id_seq")
  @Column(name = "id")
  private Long id;

  private String name;

  @OneToMany(mappedBy = "accessCriteriaSet", fetch = FetchType.LAZY)
  @Exclude
  private Set<Resource> resources;

  @OneToMany(mappedBy = "accessCriteriaSet", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @Exclude
  private Set<FormSectionLink> formLinks = new HashSet<>();

  public AccessCriteriaSet(String name) {
    this.name = name;
  }

  public Set<AccessCriteriaSection> getSections() {
    return formLinks.stream()
        .map(FormSectionLink::getAccessCriteriaSection)
        .collect(Collectors.toUnmodifiableSet());
  }

}
