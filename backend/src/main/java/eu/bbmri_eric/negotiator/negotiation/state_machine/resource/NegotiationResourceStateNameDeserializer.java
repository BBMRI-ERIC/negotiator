package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * Reads a Resource State as its name, refusing a name no State carries.
 *
 * <p>It refuses while Jackson reads the body, so the client gets a 400 for an unreadable request
 * instead of a failure later inside the update rules.
 */
public class NegotiationResourceStateNameDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    String stateName = parser.getValueAsString();
    NegotiationResourceState.valueOf(stateName);
    return stateName;
  }
}
