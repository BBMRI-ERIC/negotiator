package eu.bbmri_eric.negotiator.converters;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationEvent;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NegotiationEventConverter implements Converter<String, NegotiationEvent> {
  @Override
  public NegotiationEvent convert(String source) {
    try {
      return NegotiationEvent.valueOf(source.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
  }
}
