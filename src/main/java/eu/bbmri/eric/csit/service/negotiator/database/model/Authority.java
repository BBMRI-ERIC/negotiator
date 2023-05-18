package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "authorities")
public class Authority extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id")
  private Person person;

  private String authority;
}
