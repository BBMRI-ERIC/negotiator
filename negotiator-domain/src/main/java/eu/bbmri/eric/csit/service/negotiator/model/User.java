package eu.bbmri.eric.csit.service.negotiator.model;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "users")
public class User {

  @Id
  @Column(nullable = false, unique = true)
  private String username;

  private String password;

  private boolean enabled = true;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "username")
  private Set<Authority> authorities;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  private DataSource dataSource;
}
