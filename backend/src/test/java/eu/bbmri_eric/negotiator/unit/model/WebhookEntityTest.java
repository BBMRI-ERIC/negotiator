package eu.bbmri_eric.negotiator.unit.model;

public class WebhookEntityTest {

  //    private Webhook webhook;
  //
  //    @BeforeEach
  //    void setUp() {
  //        webhook = new Webhook("https://example.com/webhook", true, true);
  //    }
  //
  //    @Test
  //    void testAddSingleDeliveryWith200() {
  //        Delivery delivery = new Delivery(webhook, false, "{\"message\":\"Test delivery 1\"}",
  // 200, null);
  //        webhook.addDelivery(delivery);
  //        assertEquals(1, webhook.getDeliveries().size());
  //        assertSame(webhook, delivery.getWebhook());
  //        assertEquals(delivery, webhook.getDeliveries().get(0));
  //    }
  //
  //    @Test
  //    void testAddMultipleDeliveriesWithinLimit() {
  //        for (int i = 1; i <= 5; i++) {
  //            Delivery delivery = new Delivery(webhook, false, "{\"message\":\"Delivery " + i +
  // "\"}", 200, null);
  //            webhook.addDelivery(delivery);
  //        }
  //        assertEquals(5, webhook.getDeliveries().size());
  //        Delivery mostRecent = webhook.getDeliveries().get(0);
  //        assertTrue(mostRecent.getContent().contains("Delivery 5"));
  //    }
  //
  //    @Test
  //    void testAddMoreThan100Deliveries() {
  //        for (int i = 1; i <= 105; i++) {
  //            Delivery delivery = new Delivery(webhook, false, "{\"message\":\"Delivery " + i +
  // "\"}", 200, null);
  //            webhook.addDelivery(delivery);
  //        }
  //        assertEquals(100, webhook.getDeliveries().size());
  //        assertTrue(webhook.getDeliveries().get(0).getContent().contains("Delivery 105"));
  //        assertTrue(webhook.getDeliveries().get(99).getContent().contains("Delivery 6"));
  //    }
  //
  //    @Test
  //    void testAddDeliveryNon200WithoutErrorMessageThrowsException() {
  //        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
  // {
  //            new Delivery(webhook, false, "{\"message\":\"Error delivery\"}", 500, null);
  //        });
  //        assertEquals("Non-200 HTTP status code requires an error message.",
  // exception.getMessage());
  //    }
  //
  //    @Test
  //    void testAddDeliveryNon200WithErrorMessage() {
  //        Delivery delivery = new Delivery(webhook, false, "{\"message\":\"Error delivery\"}",
  // 500, "Server error");
  //        webhook.addDelivery(delivery);
  //        assertEquals(1, webhook.getDeliveries().size());
  //        assertEquals("Server error", delivery.getErrorMessage());
  //    }
}
