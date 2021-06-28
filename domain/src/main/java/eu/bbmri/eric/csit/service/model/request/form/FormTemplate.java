package eu.bbmri.eric.csit.service.model.request.form;

import eu.bbmri.eric.csit.service.model.BaseEntity;
import eu.bbmri.eric.csit.service.model.request.Request;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "form_template")
public class FormTemplate extends BaseEntity {
    private Integer requestId;

    @ManyToOne
    @JoinColumn
    private Request request;
}
