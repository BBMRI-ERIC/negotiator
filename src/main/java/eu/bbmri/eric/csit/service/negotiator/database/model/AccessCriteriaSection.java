package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.sun.istack.NotNull;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.validator.constraints.UniqueElements;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccessCriteriaSection extends BaseEntity implements Comparable<AccessCriteriaSection> {

  @NotNull
  @Column(unique=true)
  private String title;

  @NotNull
  private String description;

  @OneToMany(mappedBy = "section")
  @Exclude
  Set<AccessCriteria> accessCriteria;

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
    return Objects.equals(getTitle(), that.getTitle());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTitle());
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
