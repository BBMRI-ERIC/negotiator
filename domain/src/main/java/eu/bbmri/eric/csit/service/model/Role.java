package eu.bbmri.eric.csit.service.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
@Table(name = "role")
public class Role extends AuditEntity {

  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", insertable = false, updatable = false)
  @Exclude
  private PersonProjectLink personProjectLink;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", insertable = false, updatable = false)
  @Exclude
  private PersonRequestLink personRequestLink;
}
