package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import org.junit.jupiter.api.Test;

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
