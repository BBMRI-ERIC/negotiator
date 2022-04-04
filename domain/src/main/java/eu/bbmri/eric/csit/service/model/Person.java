package eu.bbmri.eric.csit.service.model;

import com.sun.istack.NotNull;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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

  private enum UserType {
    INTERNAL,
    EXTERNAL
  }
  @ManyToMany
  @JoinTable(
      name = "person_biobank_link",
      joinColumns = @JoinColumn(name = "biobank_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  @Exclude
  Set<Biobank> biobanks;

  @ManyToMany
  @JoinTable(
      name = "person_collection_link",
      joinColumns = @JoinColumn(name = "collection_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  @Exclude
  Set<Collection> collections;

  @ManyToMany
  @JoinTable(
      name = "person_network_link",
      joinColumns = @JoinColumn(name = "network_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  @Exclude
  Set<Network> networks;

  @ManyToMany
  @JoinTable(
      name = "person_project_link",
      joinColumns = @JoinColumn(name = "project_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  @Exclude
  Set<Project> projects;

  @Enumerated(EnumType.STRING)
  @NotNull
  private UserType type; // Type of user: local or perun

  @NotNull private String authSubject;

  @NotNull private String authName;

  @NotNull private String authEmail;

  private String password; // can be null if the user is authenticated via OIDC
  private byte[] personImage;

  @NotNull private Boolean isAdmin;

  private String organization;
}
