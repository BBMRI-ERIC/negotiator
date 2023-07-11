package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.istack.NotNull;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "Resource")
@Table(name = "resource")
public class Resource extends BaseEntity {

  private String name;

  @Column(columnDefinition = "VARCHAR(5000)")
  private String description;

  @NotNull private String sourceId;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @Exclude
  @JsonIgnore
  @NotNull
  private DataSource dataSource;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_criteria_set_id")
  @Exclude
  private AccessCriteriaSet accessCriteriaSet;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Resource resource = (Resource) o;
    return Objects.equals(sourceId, resource.sourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceId);
  }
}
