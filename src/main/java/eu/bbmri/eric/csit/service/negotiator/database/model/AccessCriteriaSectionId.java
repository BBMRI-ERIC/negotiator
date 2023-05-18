package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class AccessCriteriaSectionId implements Serializable {

  Long accessCriteria;

  Long accessCriteriaSection;
}
