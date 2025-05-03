package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationTimelineEventDTO;
import jakarta.transaction.Transactional;

import java.util.List;
public interface NegotiationTimeline {
    /**
     * Retrieve all timeline events for a Negotiation
     * @return a list of events
     */
    public List<NegotiationTimelineEventDTO> getTimelineEvents(String negotiationId);
}
