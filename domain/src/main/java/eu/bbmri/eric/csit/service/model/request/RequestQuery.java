package eu.bbmri.eric.csit.service.model.request;

import eu.bbmri.eric.csit.service.model.BaseEntity;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "query")
public class RequestQuery extends BaseEntity {

  //    @Type(type = "json")
  //    @Column(columnDefinition = "jsonb")
  //    private RequestQueryParameters requestQueryParameters;
  private Integer directoryId;
  private String requestToken;
  private String queryToken;

  @ManyToOne @JoinColumn private Request request;
}
