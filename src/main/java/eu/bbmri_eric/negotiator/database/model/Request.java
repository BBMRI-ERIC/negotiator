package eu.bbmri_eric.negotiator.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.annotations.GenericGenerator;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "Request")
@Table(name = "request")
@NamedEntityGraph(
    name = "request-with-detailed-resources",
    attributeNodes = {@NamedAttributeNode(value = "resources")})
public class Request {

  @NotNull
  @Column(columnDefinition = "TEXT")
  private String url;

  @NotNull
  @Column(columnDefinition = "TEXT")
  private String humanReadable;

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  @Column(name = "id")
  private String id;

  @ManyToMany
  @JoinTable(
      name = "request_resources_link",
      joinColumns = @JoinColumn(name = "request_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  @Exclude
  @NotNull
  private Set<Resource> resources;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "negotiation_id")
  @Exclude
  private Negotiation negotiation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @JsonIgnore
  @NotNull
  @Exclude
  private DataSource dataSource;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Request request = (Request) o;
    return Objects.equals(getId(), request.getId()) && Objects.equals(getUrl(), request.getUrl());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getUrl());
  }
}
