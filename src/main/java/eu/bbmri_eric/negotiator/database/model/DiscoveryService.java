package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * This class represents a Discovery Service entity in the database.
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "DiscoveryService")
@Table(name = "discovery_service")
public class DiscoveryService extends AuditEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @NotNull private String name;

  @NotNull
  @Column(unique = true)
  private String url;

  /**
   * Overrides the default equals method to compare DiscoveryService objects based on name and url.
   *
   * @param o the object to compare with
   * @return true if the objects are equal, false otherwise
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DiscoveryService that = (DiscoveryService) o;
    return Objects.equals(getName(), that.getName()) && Objects.equals(getUrl(), that.getUrl());
  }

  /**
   * Overrides the default hashCode method to generate a hash code based on name and url.
   *
   * @return the hash code of the object
   */
  @Override
  public int hashCode() {
    return Objects.hash(getName(), getUrl());
  }
}
