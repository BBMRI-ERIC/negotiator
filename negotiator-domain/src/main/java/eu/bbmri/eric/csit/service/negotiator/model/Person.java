package eu.bbmri.eric.csit.service.negotiator.model;

import com.sun.istack.NotNull;
import java.util.Set;
import javax.persistence.Entity;
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

  @NotNull private String authSubject;

  @NotNull private String authName;

  @NotNull private String authEmail;

  private String password; // can be null if the user is authenticated via OIDC

  private byte[] personImage;

  private String organization;
}
