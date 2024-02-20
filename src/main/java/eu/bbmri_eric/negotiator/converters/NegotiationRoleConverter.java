package eu.bbmri_eric.negotiator.converters;

import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;

public class NegotiationRoleConverter implements Converter<String, NegotiationRole> {
  @Nullable
  @Override
  public NegotiationRole convert(String source) {
    try {
      return NegotiationRole.valueOf(source.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
  }
}
