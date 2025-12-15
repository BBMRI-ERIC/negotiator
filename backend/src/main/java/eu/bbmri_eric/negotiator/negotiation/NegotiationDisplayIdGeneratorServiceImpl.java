package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.DisplayIdGenerationException;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Generates a new display_id value from the negotiation_display_id_seq sequence. Usage: Inject this
 * generator and call generateDisplayId() when creating a new Negotiation.
 */
@Service(value = "DefaultNegotiationDisplayIdGeneratorService")
@CommonsLog
public class NegotiationDisplayIdGeneratorServiceImpl
    implements NegotiationDisplayIdGeneratorService {

  private final JdbcTemplate jdbcTemplate;

  public NegotiationDisplayIdGeneratorServiceImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Fetches the next value from negotiation_display_id_seq and returns as a String display_id. Can
   * be used to generate unique display IDs for negotiations. SELECT
   * nextval('negotiation_display_id_seq')
   */
  @Override
  public String generateDisplayId(String negotiationId) {
    try {
      return String.valueOf(
          jdbcTemplate.queryForObject(
              "SELECT nextval('negotiation_display_id_seq')", Integer.class));
    } catch (Exception e) {
      log.error("Error generating display ID", e);
      throw new DisplayIdGenerationException();
    }
  }
}
