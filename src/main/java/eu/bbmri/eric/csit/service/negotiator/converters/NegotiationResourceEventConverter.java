package eu.bbmri.eric.csit.service.negotiator.converters;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NegotiationResourceEventConverter
    implements Converter<String, NegotiationResourceEvent> {
  @Override
  public NegotiationResourceEvent convert(String source) {
    try {
      return NegotiationResourceEvent.valueOf(source.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
  }
}
