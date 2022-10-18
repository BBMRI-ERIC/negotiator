package eu.bbmri.eric.csit.service.negotiator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "Query")
@Table(name = "query")
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
@NamedEntityGraph(
    name = "query-with-detailed-collections",
    attributeNodes = {
      @NamedAttributeNode(value = "collections", subgraph = "collections-with-biobank")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "collections-with-biobank",
          attributeNodes = {@NamedAttributeNode("biobank")})
    })
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
public class Query extends BaseEntity {

  //  @ManyToMany
  //  @JoinTable(
  //      name = "query_biobank_link",
  //      joinColumns = @JoinColumn(name = "biobank_id"),
  //      inverseJoinColumns = @JoinColumn(name = "query_id"))
  //  @Exclude
  //  private Set<Biobank> biobanks;

  @ManyToMany
  @JoinTable(
      name = "query_resources_link",
      joinColumns = @JoinColumn(name = "query_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  @Exclude
//  @NotNull
  private Set<Resource> resources;

  @ManyToMany
  @JoinTable(
      name = "query_collection_link",
      joinColumns = @JoinColumn(name = "query_id"),
      inverseJoinColumns = @JoinColumn(name = "collection_id"))
  @Exclude
  @NotNull
  private Set<Collection> collections;

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  @NotNull
  private String jsonPayload;

  @NotNull
  private String humanReadable;

  @NotNull
  private String url;

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
  private String token = UUID.randomUUID().toString().replace("-", "");


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
        && Objects.equals(getJsonPayload(), query.getJsonPayload())
        && Objects.equals(getUrl(), query.getUrl())
        && Objects.equals(getToken(), query.getToken());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getJsonPayload(), getUrl(), getToken());
  }
}
