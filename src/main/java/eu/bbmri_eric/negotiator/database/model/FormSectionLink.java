package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SequenceGenerator(name = "access_form_link_id_seq")
public class FormSectionLink {
  @Id
  @GeneratedValue(generator = "access_form_link_id_seq", strategy = GenerationType.SEQUENCE)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "access_criteria_set_id")
  @NaturalId
  private AccessCriteriaSet accessCriteriaSet;

  @ManyToOne
  @JoinColumn(name = "access_criteria_section_id")
  @NaturalId
  private AccessCriteriaSection accessCriteriaSection;

  @OneToMany(mappedBy = "formSectionLink")
  private Set<SectionElementLink> sectionElementLinks;
}
