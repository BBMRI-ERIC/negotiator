package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.webhook.Delivery;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WebhookEntityTest {

  private Webhook webhook;

  @BeforeEach
  void setUp() {
    webhook = new Webhook("https://example.com/webhook", true, true);
    webhook.setId(1L);
  }

  @Test
  void testAddSingleDeliveryWith200() {
    Delivery delivery = new Delivery("{\"message\":\"Test delivery 1\"}", 200);
    webhook.addDelivery(delivery);
    assertEquals(1, webhook.getDeliveries().size());
    assertEquals(webhook.getId(), delivery.getWebhookId());
    assertEquals(delivery, webhook.getDeliveries().get(0));
  }

  @Test
  void testAddMultipleDeliveriesWithinLimit() {
    for (int i = 1; i <= 5; i++) {
      Delivery delivery = new Delivery("{\"message\":\"Delivery " + i + "\"}", 200);
      webhook.addDelivery(delivery);
    }
    assertEquals(5, webhook.getDeliveries().size());
    Delivery mostRecent = webhook.getDeliveries().get(0);
    assertTrue(mostRecent.getContent().contains("Delivery 5"));
    webhook
        .getDeliveries()
        .forEach(delivery -> assertEquals(webhook.getId(), delivery.getWebhookId()));
  }

  @Test
  void testAddMoreThan100Deliveries() {
    for (int i = 1; i <= 105; i++) {
      Delivery delivery = new Delivery("{\"message\":\"Delivery " + i + "\"}", 200);
      webhook.addDelivery(delivery);
    }
    assertEquals(100, webhook.getDeliveries().size());
    assertTrue(webhook.getDeliveries().get(0).getContent().contains("Delivery 105"));
    assertTrue(webhook.getDeliveries().get(99).getContent().contains("Delivery 6"));
  }

  @Test
  void testAddDeliveryNon200WithoutErrorMessageThrowsException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new Delivery("{\"message\":\"Error delivery\"}", 500));
    assertEquals("Non-200 HTTP status code requires an error message.", exception.getMessage());
  }

  @Test
  void testAddDeliveryNon200WithErrorMessage() {
    Delivery delivery = new Delivery("{\"message\":\"Error delivery\"}", 500, "Server error");
    webhook.addDelivery(delivery);
    assertEquals(1, webhook.getDeliveries().size());
    assertEquals("Server error", delivery.getErrorMessage());
    assertEquals(webhook.getId(), delivery.getWebhookId());
  }

  @Test
  void testDeliveriesOrderedByAtDescending() {
    Delivery d1 = new Delivery("{\"message\":\"Oldest\"}", 200);
    d1.setAt(LocalDateTime.now().minusMinutes(10));
    Delivery d2 = new Delivery("{\"message\":\"Middle\"}", 200);
    d2.setAt(LocalDateTime.now().minusMinutes(5));
    Delivery d3 = new Delivery("{\"message\":\"Newest\"}", 200);
    d3.setAt(LocalDateTime.now());
    webhook.addDelivery(d1);
    webhook.addDelivery(d2);
    webhook.addDelivery(d3);
    List<Delivery> deliveries = webhook.getDeliveries();
    assertEquals(3, deliveries.size());
    assertEquals("Newest", extractMessage(deliveries.get(0)));
    assertEquals("Middle", extractMessage(deliveries.get(1)));
    assertEquals("Oldest", extractMessage(deliveries.get(2)));
    assertTrue(deliveries.get(0).getAt().isAfter(deliveries.get(1).getAt()));
    assertTrue(deliveries.get(1).getAt().isAfter(deliveries.get(2).getAt()));
  }

  // Helper to extract the message from the JSON payload
  private String extractMessage(Delivery delivery) {
    String json = delivery.getContent();
    return json.replaceAll(".*\"message\":\"(.*?)\".*", "$1");
  }
}
