package eu.bbmri.eric.csit.service.negotiator.database.model;

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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
    attributeNodes = {@NamedAttributeNode("roles")})
@SequenceGenerator(name = "person_id_seq", initialValue = 300)
public class Person {

  @ManyToMany
  @JoinTable(
      name = "resource_representative",
      joinColumns = @JoinColumn(name = "person_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  @Exclude
  Set<Resource> resources;

  boolean admin;

  @ManyToMany
  @JoinTable(
      name = "person_project_link",
      joinColumns = @JoinColumn(name = "person_id"),
      inverseJoinColumns = @JoinColumn(name = "project_id"))
  @Exclude
  Set<Project> projects;

  @OneToMany(mappedBy = "person")
  @Exclude
  Set<PersonNegotiationRole> roles = new HashSet<>();

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_id_seq")
  @Column(name = "id")
  private Long id;

  @Column(unique = true)
  @NotNull
  private String authSubject;

  @NotNull private String authName;

  @NotNull private String authEmail;

  private String password; // can be null if the user is authenticated via OIDC

  private String organization;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "person")
  private Set<Authority> authorities;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Person person = (Person) o;
    return Objects.equals(getAuthSubject(), person.getAuthSubject())
        && Objects.equals(getAuthName(), person.getAuthName())
        && Objects.equals(getAuthEmail(), person.getAuthEmail())
        && Objects.equals(getPassword(), person.getPassword())
        && Objects.equals(getOrganization(), person.getOrganization());
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
            getAuthSubject(), getAuthName(), getAuthEmail(), getPassword(), getOrganization());
    result = 31 * result;
    return result;
  }
}
