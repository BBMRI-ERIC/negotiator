package eu.bbmri.eric.csit.service.negotiator.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "person_request_link")
public class PersonRequestLink extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id")
  @Exclude
  private Person person;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
  @Exclude
  private Request request;

  private Integer roleId;
}
