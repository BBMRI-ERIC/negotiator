package eu.bbmri.eric.csit.service.negotiator.unit.model;

import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class RequestTest {
  @Test
  void testInitRequest() {
    assertInstanceOf(Request.class, new Request());
  }
}
