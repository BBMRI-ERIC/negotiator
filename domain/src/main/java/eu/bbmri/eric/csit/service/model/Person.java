package eu.bbmri.eric.csit.service.model;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "person")
public class Person extends BaseEntity {

  private String authSubject;
  private String authName;
  private String authEmail;
  private byte[] personImage;
  private Boolean isAdmin;
  private String organization;
  @ManyToMany
  @JoinTable(
      name = "person_biobank_link",
      joinColumns = @JoinColumn(name = "biobank_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  Set<Biobank> biobanks ;
  @ManyToMany
  @JoinTable(
      name = "person_collection_link",
      joinColumns = @JoinColumn(name = "collection_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  Set<Collection> collections ;
  @ManyToMany
  @JoinTable(
      name = "person_network_link",
      joinColumns = @JoinColumn(name = "network_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  Set<Network> networks;
  @ManyToMany
  @JoinTable(
      name = "person_project_link",
      joinColumns = @JoinColumn(name = "project_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  Set<Project> projects;
}
