package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import eu.bbmri_eric.negotiator.database.model.Request;
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
