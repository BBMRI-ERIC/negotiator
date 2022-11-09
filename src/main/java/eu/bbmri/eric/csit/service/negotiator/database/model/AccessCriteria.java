package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.sun.istack.NotNull;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
public class AccessCriteria extends BaseEntity {

  @NotNull
  private String name;

  @NotNull
  private String description;

  @NotNull
  private String type;

  @NotNull private Boolean required;

  @OneToMany(mappedBy = "accessCriteria")
  @Exclude
  Set<AccessCriteriaSetLink> accessCriteriaSetLink;
}
