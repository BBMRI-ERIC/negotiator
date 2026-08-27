package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * Reads a Resource State as its name, refusing a name no State carries.
 *
 * <p>Keeps the 400 a request body earned when the field was typed as the enum: the refusal happens
 * while the body is being read, so it is reported as an unreadable request rather than surfacing
 * later as a failure inside the update rules.
 *
 * <p>Delete this class at the Lifecycle cutover, along with the enum it consults. What replaces it
 * is a check for the named {@code state} row in the Definition Version the Resource is pinned to.
 */
public class NegotiationResourceStateNameDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    String stateName = parser.getValueAsString();
    NegotiationResourceState.valueOf(stateName);
    return stateName;
  }
}
