package eu.bbmri_eric.negotiator.negotiation;

/** Service interface for generating display IDs for negotiations. */
public interface NegotiationDisplayIdGeneratorService {
  String generateDisplayId(String negotiationId);
}
