package eu.bbmri.eric.csit.service.negotiator.unit.model;


import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RequestTest {
  @Test
  void testInitRequest() {
    assertInstanceOf(Request.class, new Request());
  }

  @Test
  void equals_sameId_equal() {
    assertEquals(Request.builder().id("test").build(), Request.builder().id("test").build());
  }

  @Test
  void equals_differentId_notEqual() {
    assertNotEquals(Request.builder().id("test").build(), Request.builder().id("notTest").build());
  }

  @Test
  void equals_idIsNull_equal() {
    assertEquals(new Request(), new Request());
  }
}
