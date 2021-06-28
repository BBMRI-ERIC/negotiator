package eu.bbmri.eric.csit.service.model.request;

import eu.bbmri.eric.csit.service.model.BaseEntity;
import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Data
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "query")
public class RequestQuery extends BaseEntity {

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private RequestQueryParameters requestQueryParameters;
    private Integer directoryId;
    private String requestToken;
    private String queryToken;

    @ManyToOne
    @JoinColumn
    private Request request;
}
