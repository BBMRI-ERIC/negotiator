package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(name = "section_element_link_id_seq")
public class SectionElementLink {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "section_element_link_id_seq")
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "form_section_link_id")
  private FormSectionLink formSectionLink;

  @ManyToOne
  @JoinColumn(name = "access_criteria_id")
  private AccessCriteria accessCriteria;

  private boolean isRequired;
}
