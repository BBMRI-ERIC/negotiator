package eu.bbmri.eric.csit.service.negotiator.database.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
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
@IdClass(AccessCriteriaTemplateId.class)
public class AccessCriteriaTemplateLink {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id")
  @Exclude
  @Id
  private AccessCriteriaTemplate template;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_criteria_id")
  @Exclude
  @Id
  private AccessCriteria accessCriteria;

  @NotNull
  private Integer ordering;
}
