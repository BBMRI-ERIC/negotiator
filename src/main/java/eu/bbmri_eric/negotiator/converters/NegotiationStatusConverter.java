package eu.bbmri_eric.negotiator.converters;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;

public class NegotiationStatusConverter implements Converter<String, NegotiationState> {
  @Nullable
  @Override
  public NegotiationState convert(String source) {
    try {
      return NegotiationState.valueOf(source.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
  }
}
