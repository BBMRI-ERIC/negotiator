package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
@NamedEntityGraph(
    name = "person-detailed",
    attributeNodes = {
      @NamedAttributeNode(value = "roles"),
    })
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

  @OneToMany(mappedBy = "person")
  @Exclude
  Set<PersonNegotiationRole> roles = new HashSet<>();

  @NotNull private String name;

  @NotNull private String email;

  private String password; // can be null if the user is authenticated via OIDC

  private String organization;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "person")
  private Set<Authority> authorities;

  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "network_person_link",
      joinColumns = @JoinColumn(name = "person_id"),
      inverseJoinColumns = @JoinColumn(name = "network_id"))
  @Exclude
  @Setter(AccessLevel.NONE)
  private Set<Network> networks;

  public void addResource(Resource resource) {
    this.resources.add(resource);
    resource.getRepresentatives().add(this);
  }

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
}
