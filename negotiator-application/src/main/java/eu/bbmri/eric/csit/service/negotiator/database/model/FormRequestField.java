package eu.bbmri.eric.csit.service.negotiator.database.model;

import javax.persistence.Column;
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
@Table(name = "form_request_field")
public class FormRequestField extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "form_request_id")
  @Exclude
  private FormRequest formRequest;

  @Column(name = "\"order\"")
  private Integer order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "form_field_id")
  @Exclude
  private FormField formField;
}
