package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@IdClass(AccessCriteriaSectionId.class)
public class AccessCriteriaSectionLink extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_criteria_id")
  @Exclude
  @Id
  private AccessCriteria accessCriteria;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_criteria_section_id")
  @Exclude
  @Id
  private AccessCriteriaSection accessCriteriaSection;

  @NotNull private Integer ordering;

  @NotNull private Boolean required;
}
