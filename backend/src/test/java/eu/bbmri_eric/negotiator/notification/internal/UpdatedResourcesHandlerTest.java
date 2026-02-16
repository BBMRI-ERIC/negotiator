package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdatedResourcesHandlerTest {

  @Mock private ResourceNotificationService resourceNotificationService;

  private UpdatedResourcesHandler handler;

  @BeforeEach
  void setUp() {
    handler = new UpdatedResourcesHandler(resourceNotificationService);
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    Class<NewResourcesAddedEvent> eventType = handler.getSupportedEventType();
    assertEquals(NewResourcesAddedEvent.class, eventType);
  }

  @Test
  void notify_CallsResourceNotificationService() {
    String negotiationId = "123";
    NewResourcesAddedEvent event = new NewResourcesAddedEvent(this, negotiationId);

    handler.notify(event);

    verify(resourceNotificationService).notifyResourceRepresentatives(negotiationId);
  }

  @Test
  void notify_WithDifferentNegotiationId_CallsServiceWithCorrectId() {
    String negotiationId = "456";
    NewResourcesAddedEvent event = new NewResourcesAddedEvent(this, negotiationId);

    handler.notify(event);

    verify(resourceNotificationService).notifyResourceRepresentatives(negotiationId);
  }
}
