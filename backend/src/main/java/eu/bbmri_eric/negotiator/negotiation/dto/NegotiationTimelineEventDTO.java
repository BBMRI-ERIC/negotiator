package eu.bbmri_eric.negotiator.negotiation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Relation(itemRelation = "timelineEvent", collectionRelation = "timelineEvents")
public class NegotiationTimelineEventDTO {
    String triggeredBy;
    String text;
    LocalDateTime timestamp;
}
