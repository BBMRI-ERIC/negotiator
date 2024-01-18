package eu.bbmri.eric.csit.service.negotiator.database.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "person_negotiation_role")
@IdClass(PersonNegotiationRoleId.class)
public class PersonNegotiationRole extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
  @JoinColumn(name = "person_id")
  @Id
  private Person person;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "negotiation_id")
  @Id
  private Negotiation negotiation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id")
  @Id
  private Role role;
}
