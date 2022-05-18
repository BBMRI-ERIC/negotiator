package eu.bbmri.eric.csit.service.negotiator.model;

import com.sun.istack.NotNull;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
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
@Table(name = "person")
public class Person extends BaseEntity {

  @ManyToMany
  @JoinTable(
      name = "person_biobank_link",
      joinColumns = @JoinColumn(name = "person_id"),
      inverseJoinColumns = @JoinColumn(name = "biobank_id"))
  @Exclude
  Set<Biobank> biobanks;

  @ManyToMany
  @JoinTable(
      name = "person_collection_link",
      joinColumns = @JoinColumn(name = "person_id"),
      inverseJoinColumns = @JoinColumn(name = "collection_id"))
  @Exclude
  Set<Collection> collections;

  @ManyToMany
  @JoinTable(
      name = "person_network_link",
      joinColumns = @JoinColumn(name = "person_id"),
      inverseJoinColumns = @JoinColumn(name = "network_id"))
  @Exclude
  Set<Network> networks;

  @ManyToMany
  @JoinTable(
      name = "person_project_link",
      joinColumns = @JoinColumn(name = "person_id"),
      inverseJoinColumns = @JoinColumn(name = "project_id"))
  @Exclude
  Set<Project> projects;

  @OneToMany(mappedBy = "person")
  @Exclude
  Set<PersonRequestRole> roles;

  @NotNull private String authSubject;

  @NotNull private String authName;

  @NotNull private String authEmail;

  private String password; // can be null if the user is authenticated via OIDC

  private byte[] personImage;

  private String organization;

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
        && Arrays.equals(getPersonImage(), person.getPersonImage())
        && Objects.equals(getOrganization(), person.getOrganization());
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
            getAuthSubject(), getAuthName(), getAuthEmail(), getPassword(), getOrganization());
    result = 31 * result + Arrays.hashCode(getPersonImage());
    return result;
  }
}
