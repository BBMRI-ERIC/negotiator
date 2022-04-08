package eu.bbmri.eric.csit.service.model;

import com.sun.istack.NotNull;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@ToString
@AllArgsConstructor
@Getter
@Setter
@Entity(name = "Query")
@Table(name = "query")
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
public class Query extends BaseEntity {

  public Query() {
    this.setQueryToken(generateQueryToken());
  }

  private String generateQueryToken() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  @ManyToMany
  @JoinTable(
      name = "query_biobank_link",
      joinColumns = @JoinColumn(name = "biobank_id"),
      inverseJoinColumns = @JoinColumn(name = "query_id"))
  @Exclude
  @NotNull
  private Set<Biobank> biobanks;

  @ManyToMany
  @JoinTable(
      name = "query_collection_link",
      joinColumns = @JoinColumn(name = "collection_id"),
      inverseJoinColumns = @JoinColumn(name = "query_id"))
  @Exclude
  @NotNull
  private Set<Collection> collections;

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  @NotNull
  private String jsonPayload;

  @NotNull private String queryToken;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
  @Exclude
  private Request request;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @Exclude
  private DataSource dataSource;
}
