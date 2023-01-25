package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.io.Serializable;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
