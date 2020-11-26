package eu.bbmri.eric.csit.service.model.request;

import eu.bbmri.eric.csit.service.model.BaseEntity;
import eu.bbmri.eric.csit.service.model.request.form.FormTemplate;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

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
    private String request_description;
    private String ethics_vote;
    private Boolean test_request;

    @OneToMany(mappedBy = "request")
    private Set<FormTemplate> formTemplates = new HashSet<>();
}
