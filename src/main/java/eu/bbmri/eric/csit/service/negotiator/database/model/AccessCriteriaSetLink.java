package eu.bbmri.eric.csit.service.negotiator.database.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
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
@IdClass(AccessCriteriaSetId.class)
public class AccessCriteriaSetLink {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_criteria_id")
  @Exclude
  @Id
  private AccessCriteria accessCriteria;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_criteria_set_id")
  @Exclude
  @Id
  private AccessCriteriaSet accessCriteriaSet;

  @NotNull
  private Integer ordering;
}
