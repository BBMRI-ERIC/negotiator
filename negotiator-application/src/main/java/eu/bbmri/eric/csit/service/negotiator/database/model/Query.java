package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "Query")
@Table(name = "query")
@NamedEntityGraph(
    name = "query-with-detailed-resources",
    attributeNodes = {
        @NamedAttributeNode(value = "resources", subgraph = "resources-with-parent")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "resources-with-parent",
            attributeNodes = {@NamedAttributeNode("parent")})
    })
public class Query {

  @NotNull
  private String url;

  @NotNull
  private String humanReadable;

  @Id
  @Column(name = "id")
  private final String id = UUID.randomUUID().toString().replace("-", "");;

  @ManyToMany
  @JoinTable(
      name = "query_resources_link",
      joinColumns = @JoinColumn(name = "query_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  @Exclude
  @NotNull
  private Set<Resource> resources;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
  @Exclude
  private Request request;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @JsonIgnore
  @NotNull
  @Exclude
  private DataSource dataSource;

  @NotNull
  private final String token = UUID.randomUUID().toString().replace("-", "");

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Query query = (Query) o;
    return Objects.equals(getId(), query.getId())
        && Objects.equals(getUrl(), query.getUrl())
        && Objects.equals(getToken(), query.getToken());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getUrl(), getToken());
  }
}
