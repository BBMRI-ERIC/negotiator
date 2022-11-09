package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.sun.istack.NotNull;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccessCriteria extends BaseEntity {

  @NotNull
  private String name;

  @NotNull
  private String description;

  private String type;

  @NotNull private Boolean required;

  @OneToMany(mappedBy = "accessCriteria")
  Set<AccessCriteriaTemplateLink> templates;
}
