package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;

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
    attributeNodes = {
        @NamedAttributeNode(value = "resources", subgraph = "resources-with-parent")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "resources-with-parent",
            attributeNodes = {@NamedAttributeNode("parent")})
    })
public class Request {

  @NotNull
  private String url;

  @NotNull
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
    return Objects.equals(getId(), request.getId())
        && Objects.equals(getUrl(), request.getUrl());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getUrl());
  }
}
