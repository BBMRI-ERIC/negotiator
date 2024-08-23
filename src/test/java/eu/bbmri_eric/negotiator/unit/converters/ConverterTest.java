package eu.bbmri_eric.negotiator.unit.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationRoleConverter;
import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationStatusConverter;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

public class ConverterTest {

  @Test
  public void testConvert() {
    NegotiationRoleConverter converter = new NegotiationRoleConverter();
    assertThrows(
        ResponseStatusException.class,
        () -> {
          converter.convert("test");
        });
  }

  @Test
  void testStatusConverter() {
    NegotiationStatusConverter converter = new NegotiationStatusConverter();
    assertThrows(
        ResponseStatusException.class,
        () -> {
          converter.convert("test");
        });
    assertEquals("APPROVED", converter.convert("APPROVED").toString());
  }
}
