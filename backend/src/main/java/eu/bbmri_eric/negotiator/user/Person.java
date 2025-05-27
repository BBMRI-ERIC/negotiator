package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString.Exclude;

@Entity
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "person")
@SequenceGenerator(name = "person_id_seq", initialValue = 10000)
public class Person {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_id_seq")
  private Long id;

  @Column(unique = true)
  @NotNull
  private String subjectId; // OIDC subject id

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "resource_representative_link",
      joinColumns = @JoinColumn(name = "person_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  @Exclude
  Set<Resource> resources = new HashSet<>();

  @Column(name = "admin", nullable = false, columnDefinition = "boolean default false")
  boolean admin;

  @Column(nullable = false, columnDefinition = "boolean default false")
  boolean isServiceAccount;

  @NotNull private String name;

  @NotNull private String email;

  private String password; // can be null if the user is authenticated via OIDC

  private String organization;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "person")
  private Set<Authority> authorities;

  @ManyToMany(mappedBy = "managers", fetch = FetchType.EAGER)
  @Exclude
  @Setter(AccessLevel.NONE)
  @Builder.Default
  private Set<Network> networks = new HashSet<>();

  @Column(name = "last_login")
  private LocalDateTime lastLogin;

  public void addResource(Resource resource) {
    this.resources.add(resource);
    resource.getRepresentatives().add(this);
  }

  /**
   * Get Resources Represented by the user.
   *
   * @return an Unmodifiable set of Resources.
   */
  public Set<Resource> getResources() {
    if (Objects.isNull(this.resources)) {
      return Set.of();
    }
    return Collections.unmodifiableSet(this.resources);
  }

  public void removeResource(Resource resource) {
    this.resources.remove(resource);
    resource.getRepresentatives().remove(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof Person)) return false;
    Person person = (Person) o;
    return Objects.equals(subjectId, person.getSubjectId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectId);
  }

  @Override
  public String toString() {
    return "Person{"
        + "id="
        + id
        + ", subjectId='"
        + subjectId
        + '\''
        + ", admin="
        + admin
        + ", isServiceAccount="
        + isServiceAccount
        + ", name='"
        + name
        + '\''
        + ", email='"
        + email
        + '\''
        + ", password='"
        + password
        + '\''
        + ", organization='"
        + organization
        + '\''
        + ", lastLogin="
        + lastLogin
        + '}';
  }
}
