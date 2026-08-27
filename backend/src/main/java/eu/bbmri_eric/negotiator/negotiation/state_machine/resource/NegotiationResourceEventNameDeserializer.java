package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class NegotiationResourceEventNameDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    String eventName = parser.getValueAsString();
    NegotiationResourceEvent.valueOf(eventName);
    return eventName;
  }
}
