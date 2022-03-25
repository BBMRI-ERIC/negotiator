package eu.bbmri.eric.csit.service.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Data
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "query")
@TypeDefs({
    @TypeDef(name = "json", typeClass = JsonType.class)
})
public class Query extends BaseEntity {

  //TODO: Hibernate does not support postgres jsonb datatype; shall we implement a custom one?
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private Person createdBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by", insertable = false, updatable = false)
  private Person modifiedBy;
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private String jsonPayload;
  private Integer datasourceId;
  private String queryToken;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
  private Request request;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  private Datasource datasource;

}
