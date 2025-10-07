package eu.bbmri_eric.negotiator.negotiation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Generates a new display_id value from the negotiation_display_id_seq sequence. Usage: Inject this
 * generator and call generateDisplayId() when creating a new Negotiation.
 */
@Component
public class NegotiationDisplayIdGenerator {

  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public NegotiationDisplayIdGenerator(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Fetches the next value from negotiation_display_id_seq and returns as a String display_id. Can
   * be used to generate unique display IDs for negotiations.
   */
  public String generateDisplayId() {
    int nextVal =
        jdbcTemplate.queryForObject("SELECT nextval('negotiation_display_id_seq')", Integer.class);
    return String.valueOf(nextVal);
  }
}
