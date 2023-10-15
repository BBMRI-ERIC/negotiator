package eu.bbmri_eric.negotiator.database.model;

import com.sun.istack.NotNull;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
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
public class AccessCriteriaSection implements Comparable<AccessCriteriaSection> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @OneToMany(mappedBy = "accessCriteriaSection")
  @OrderBy("ordering ASC")
  @Exclude
  private List<AccessCriteriaSectionLink> accessCriteriaSectionLink;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_criteria_set_id")
  @Exclude
  private AccessCriteriaSet accessCriteriaSet;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccessCriteriaSection that = (AccessCriteriaSection) o;
    return Objects.equals(getName(), that.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName());
  }

  @Override
  public int compareTo(AccessCriteriaSection section) {
    if (this.getId() < section.getId()) {
      return -1;
    } else if (this.getId().equals(section.getId())) {
      return 0;
    } else {
      return 1;
    }
  }
}
