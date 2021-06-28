package eu.bbmri.eric.csit.service.model.request;

import eu.bbmri.eric.csit.service.model.BaseEntity;
import eu.bbmri.eric.csit.service.model.request.form.FormTemplate;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private String request_token;

    @OneToMany(
            mappedBy = "requestToken",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = false
    )
    private List<RequestQuery> requestQueries = new ArrayList<>();

    @OneToMany(
            mappedBy = "requestId",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL
    )
    private List<FormTemplate> formTemplates = new ArrayList<>();
}
