package eu.bbmri.eric.csit.service.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
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
@Table(name = "request")
public class Request extends BaseEntity {

  private String title;
  @Lob
  private String requestDescription;
  @Lob
  private String ethicsVote;
  private Boolean testRequest;
  private String requestToken;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

//    @OneToMany(
//            mappedBy = "requestToken",
//            fetch = FetchType.EAGER,
//            cascade = CascadeType.ALL,
//            orphanRemoval = false
//    )
//    private List<RequestQuery> requestQueries = new ArrayList<>();
//
//    @OneToMany(
//            mappedBy = "requestId",
//            fetch = FetchType.EAGER,
//            cascade = CascadeType.ALL
//    )
//    private List<FormTemplate> formTemplates = new ArrayList<>();
}
