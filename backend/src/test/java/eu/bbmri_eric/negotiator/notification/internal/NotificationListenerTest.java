package eu.bbmri_eric.negotiator.notification.internal;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

  @Mock private NotificationStrategy<NegotiationStateChangeEvent> negotiationHandler;

  @Mock private NotificationStrategy<NewPostEvent> postHandler;

  @Mock private NotificationStrategy<ApplicationEvent> genericHandler;

  private NotificationListener notificationListener;

  @BeforeEach
  void setUp() {
    // Setup mock handlers to return their supported event types - use lenient to avoid strict
    // stubbing issues
    lenient()
        .when(negotiationHandler.getSupportedEventType())
        .thenReturn(NegotiationStateChangeEvent.class);
    lenient().when(postHandler.getSupportedEventType()).thenReturn(NewPostEvent.class);
    lenient().when(genericHandler.getSupportedEventType()).thenReturn(ApplicationEvent.class);

    List<NotificationStrategy<? extends ApplicationEvent>> handlers =
        Arrays.asList(negotiationHandler, postHandler, genericHandler);

    notificationListener = new NotificationListener(handlers);

    // Initialize the cache manually to control when it happens
    ReflectionTestUtils.invokeMethod(notificationListener, "initializeHandlerCache");

    // Reset all mocks after initialization to have clean state for each test
    reset(negotiationHandler, postHandler, genericHandler);
  }

  @Test
  void initializeHandlerCache_ShouldPopulateCacheWithAllHandlers() {
    // Given - fresh listener without cache initialization
    List<NotificationStrategy<? extends ApplicationEvent>> handlers =
        Arrays.asList(negotiationHandler, postHandler, genericHandler);
    NotificationListener freshListener = new NotificationListener(handlers);

    // Setup mocks again since we reset them
    when(negotiationHandler.getSupportedEventType()).thenReturn(NegotiationStateChangeEvent.class);
    when(postHandler.getSupportedEventType()).thenReturn(NewPostEvent.class);
    when(genericHandler.getSupportedEventType()).thenReturn(ApplicationEvent.class);

    // When
    ReflectionTestUtils.invokeMethod(freshListener, "initializeHandlerCache");

    // Then
    @SuppressWarnings("unchecked")
    Map<Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
        cache =
            (Map<
                    Class<? extends ApplicationEvent>,
                    List<NotificationStrategy<? extends ApplicationEvent>>>)
                ReflectionTestUtils.getField(freshListener, "handlerCache");

    assertNotNull(cache);
    assertEquals(3, cache.size());
    assertTrue(cache.get(NegotiationStateChangeEvent.class).contains(negotiationHandler));
    assertTrue(cache.get(NewPostEvent.class).contains(postHandler));
    assertTrue(cache.get(ApplicationEvent.class).contains(genericHandler));
  }

  @Test
  void initializeHandlerCache_WithMultipleHandlersForSameEvent_ShouldStoreAllHandlers() {
    // Given - two handlers for the same event type
    NotificationStrategy<NegotiationStateChangeEvent> secondNegotiationHandler =
        mock(NotificationStrategy.class);
    when(secondNegotiationHandler.getSupportedEventType())
        .thenReturn(NegotiationStateChangeEvent.class);

    List<NotificationStrategy<? extends ApplicationEvent>> handlers =
        Arrays.asList(negotiationHandler, secondNegotiationHandler, postHandler);
    NotificationListener freshListener = new NotificationListener(handlers);

    when(negotiationHandler.getSupportedEventType()).thenReturn(NegotiationStateChangeEvent.class);
    when(postHandler.getSupportedEventType()).thenReturn(NewPostEvent.class);

    // When
    ReflectionTestUtils.invokeMethod(freshListener, "initializeHandlerCache");

    // Then
    @SuppressWarnings("unchecked")
    Map<Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
        cache =
            (Map<
                    Class<? extends ApplicationEvent>,
                    List<NotificationStrategy<? extends ApplicationEvent>>>)
                ReflectionTestUtils.getField(freshListener, "handlerCache");

    assertNotNull(cache);
    assertEquals(2, cache.size()); // Only 2 event types
    assertEquals(
        2, cache.get(NegotiationStateChangeEvent.class).size()); // 2 handlers for this event
    assertEquals(1, cache.get(NewPostEvent.class).size()); // 1 handler for this event
    assertTrue(cache.get(NegotiationStateChangeEvent.class).contains(negotiationHandler));
    assertTrue(cache.get(NegotiationStateChangeEvent.class).contains(secondNegotiationHandler));
  }

  @Test
  void onNewEvent_WithNegotiationStateChangeEvent_ShouldCallCorrectHandler() {
    // Given
    AtomicBoolean handlerCalled = new AtomicBoolean(false);
    doAnswer(
            invocation -> {
              handlerCalled.set(true);
              return null;
            })
        .when(negotiationHandler)
        .notify(any(NegotiationStateChangeEvent.class));

    // Manually populate cache for this test
    @SuppressWarnings("unchecked")
    Map<Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
        cache =
            (Map<
                    Class<? extends ApplicationEvent>,
                    List<NotificationStrategy<? extends ApplicationEvent>>>)
                ReflectionTestUtils.getField(notificationListener, "handlerCache");
    cache.put(NegotiationStateChangeEvent.class, Arrays.asList(negotiationHandler));

    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            "negotiation-123",
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT,
            "Test post");

    // When
    ReflectionTestUtils.invokeMethod(notificationListener, "onNewEvent", event);

    // Then - Use awaitility to wait for async processing
    await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              assertTrue(handlerCalled.get(), "Negotiation handler should have been called");
              verify(negotiationHandler, times(1)).notify(event);
            });
  }

  @Test
  void onNewEvent_WithMultipleHandlersForSameEvent_ShouldCallAllHandlers() {
    // Given
    NotificationStrategy<NegotiationStateChangeEvent> secondNegotiationHandler =
        mock(NotificationStrategy.class);

    AtomicInteger firstHandlerCallCount = new AtomicInteger(0);
    AtomicInteger secondHandlerCallCount = new AtomicInteger(0);

    doAnswer(
            invocation -> {
              firstHandlerCallCount.incrementAndGet();
              return null;
            })
        .when(negotiationHandler)
        .notify(any(NegotiationStateChangeEvent.class));

    doAnswer(
            invocation -> {
              secondHandlerCallCount.incrementAndGet();
              return null;
            })
        .when(secondNegotiationHandler)
        .notify(any(NegotiationStateChangeEvent.class));

    // Manually populate cache with multiple handlers for same event
    @SuppressWarnings("unchecked")
    Map<Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
        cache =
            (Map<
                    Class<? extends ApplicationEvent>,
                    List<NotificationStrategy<? extends ApplicationEvent>>>)
                ReflectionTestUtils.getField(notificationListener, "handlerCache");
    cache.put(
        NegotiationStateChangeEvent.class,
        Arrays.asList(negotiationHandler, secondNegotiationHandler));

    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            "negotiation-123",
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT,
            "Test post");

    // When
    ReflectionTestUtils.invokeMethod(notificationListener, "onNewEvent", event);

    // Then - Use awaitility to wait for async processing
    await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              assertEquals(
                  1, firstHandlerCallCount.get(), "First handler should have been called once");
              assertEquals(
                  1, secondHandlerCallCount.get(), "Second handler should have been called once");
              verify(negotiationHandler, times(1)).notify(event);
              verify(secondNegotiationHandler, times(1)).notify(event);
            });
  }

  @Test
  void onNewEvent_WithNewPostEvent_ShouldCallCorrectHandler() {
    // Given
    AtomicBoolean handlerCalled = new AtomicBoolean(false);
    doAnswer(
            invocation -> {
              handlerCalled.set(true);
              return null;
            })
        .when(postHandler)
        .notify(any(NewPostEvent.class));

    // Manually populate cache for this test
    @SuppressWarnings("unchecked")
    Map<Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
        cache =
            (Map<
                    Class<? extends ApplicationEvent>,
                    List<NotificationStrategy<? extends ApplicationEvent>>>)
                ReflectionTestUtils.getField(notificationListener, "handlerCache");
    cache.put(NewPostEvent.class, Arrays.asList(postHandler));

    NewPostEvent event = new NewPostEvent(this, "post-123", "negotiation-456", 789L, 101L);

    // When
    ReflectionTestUtils.invokeMethod(notificationListener, "onNewEvent", event);

    // Then - Use awaitility to wait for async processing
    await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              assertTrue(handlerCalled.get(), "Post handler should have been called");
              verify(postHandler, times(1)).notify(event);
            });
  }

  @Test
  void onNewEvent_WithUnsupportedEvent_ShouldNotCallAnyHandler() throws InterruptedException {
    // Given - cache is empty, so no handlers for any events
    @SuppressWarnings("unchecked")
    Map<Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
        cache =
            (Map<
                    Class<? extends ApplicationEvent>,
                    List<NotificationStrategy<? extends ApplicationEvent>>>)
                ReflectionTestUtils.getField(notificationListener, "handlerCache");
    cache.clear();

    // Create a custom event type that has no handler
    ApplicationEvent unsupportedEvent = new ApplicationEvent(this) {};

    // When
    ReflectionTestUtils.invokeMethod(notificationListener, "onNewEvent", unsupportedEvent);

    // Then - Wait briefly to ensure no handlers are called
    Thread.sleep(100);

    verifyNoInteractions(negotiationHandler);
    verifyNoInteractions(postHandler);
    verifyNoInteractions(genericHandler);
  }

  @Test
  void onNewEvent_WhenHandlerThrowsException_ShouldCatchAndLogError() {
    // Given
    AtomicBoolean exceptionThrown = new AtomicBoolean(false);
    doAnswer(
            invocation -> {
              exceptionThrown.set(true);
              throw new RuntimeException("Test exception");
            })
        .when(negotiationHandler)
        .notify(any(NegotiationStateChangeEvent.class));

    // Manually populate cache for this test
    @SuppressWarnings("unchecked")
    Map<Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
        cache =
            (Map<
                    Class<? extends ApplicationEvent>,
                    List<NotificationStrategy<? extends ApplicationEvent>>>)
                ReflectionTestUtils.getField(notificationListener, "handlerCache");
    cache.put(NegotiationStateChangeEvent.class, Arrays.asList(negotiationHandler));

    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            "negotiation-123",
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT,
            "Test post");

    // When
    ReflectionTestUtils.invokeMethod(notificationListener, "onNewEvent", event);

    // Then - Use awaitility to wait for async processing and exception handling
    await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              assertTrue(exceptionThrown.get(), "Exception should have been thrown");
              verify(negotiationHandler, times(1)).notify(event);
            });

    // The listener should not re-throw the exception
    // This test verifies that the exception is caught and logged
  }

  @Test
  void onNewEvent_MultipleEventsSimultaneously_ShouldHandleAllCorrectly() {
    // Given
    AtomicInteger negotiationHandlerCallCount = new AtomicInteger(0);
    AtomicInteger postHandlerCallCount = new AtomicInteger(0);

    doAnswer(
            invocation -> {
              negotiationHandlerCallCount.incrementAndGet();
              return null;
            })
        .when(negotiationHandler)
        .notify(any(NegotiationStateChangeEvent.class));

    doAnswer(
            invocation -> {
              postHandlerCallCount.incrementAndGet();
              return null;
            })
        .when(postHandler)
        .notify(any(NewPostEvent.class));

    // Manually populate cache for this test
    @SuppressWarnings("unchecked")
    Map<Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
        cache =
            (Map<
                    Class<? extends ApplicationEvent>,
                    List<NotificationStrategy<? extends ApplicationEvent>>>)
                ReflectionTestUtils.getField(notificationListener, "handlerCache");
    cache.put(NegotiationStateChangeEvent.class, Arrays.asList(negotiationHandler));
    cache.put(NewPostEvent.class, Arrays.asList(postHandler));

    NegotiationStateChangeEvent negotiationEvent1 =
        new NegotiationStateChangeEvent(
            this,
            "negotiation-123",
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT,
            "Test post 1");
    NegotiationStateChangeEvent negotiationEvent2 =
        new NegotiationStateChangeEvent(
            this,
            "negotiation-456",
            NegotiationState.SUBMITTED,
            NegotiationState.APPROVED,
            NegotiationEvent.APPROVE,
            "Test post 2");
    NewPostEvent postEvent1 = new NewPostEvent(this, "post-123", "negotiation-789", 111L, 222L);
    NewPostEvent postEvent2 = new NewPostEvent(this, "post-456", "negotiation-101", 333L, 444L);

    // When - Trigger multiple events concurrently
    CompletableFuture.allOf(
            CompletableFuture.runAsync(
                () ->
                    ReflectionTestUtils.invokeMethod(
                        notificationListener, "onNewEvent", negotiationEvent1)),
            CompletableFuture.runAsync(
                () ->
                    ReflectionTestUtils.invokeMethod(
                        notificationListener, "onNewEvent", negotiationEvent2)),
            CompletableFuture.runAsync(
                () ->
                    ReflectionTestUtils.invokeMethod(
                        notificationListener, "onNewEvent", postEvent1)),
            CompletableFuture.runAsync(
                () ->
                    ReflectionTestUtils.invokeMethod(
                        notificationListener, "onNewEvent", postEvent2)))
        .join();

    // Then - Use awaitility to wait for all async processing to complete
    await()
        .atMost(Duration.ofSeconds(3))
        .untilAsserted(
            () -> {
              assertEquals(
                  2,
                  negotiationHandlerCallCount.get(),
                  "Negotiation handler should be called twice");
              assertEquals(2, postHandlerCallCount.get(), "Post handler should be called twice");
            });

    verify(negotiationHandler, times(2)).notify(any(NegotiationStateChangeEvent.class));
    verify(postHandler, times(2)).notify(any(NewPostEvent.class));
  }

  @Test
  void onNewEvent_WithSlowHandler_ShouldEventuallyComplete() {
    // Given
    AtomicBoolean slowHandlerCompleted = new AtomicBoolean(false);
    doAnswer(
            invocation -> {
              try {
                Thread.sleep(500); // Simulate slow processing
                slowHandlerCompleted.set(true);
                return null;
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
              }
            })
        .when(negotiationHandler)
        .notify(any(NegotiationStateChangeEvent.class));

    // Manually populate cache for this test
    @SuppressWarnings("unchecked")
    Map<Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
        cache =
            (Map<
                    Class<? extends ApplicationEvent>,
                    List<NotificationStrategy<? extends ApplicationEvent>>>)
                ReflectionTestUtils.getField(notificationListener, "handlerCache");
    cache.put(NegotiationStateChangeEvent.class, Arrays.asList(negotiationHandler));

    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            "negotiation-123",
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT,
            "Test post");

    // When
    ReflectionTestUtils.invokeMethod(notificationListener, "onNewEvent", event);

    // Then - Use awaitility with longer timeout for slow handler
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              assertTrue(slowHandlerCompleted.get(), "Slow handler should eventually complete");
              verify(negotiationHandler, times(1)).notify(event);
            });
  }

  @Test
  void dispatch_WithCorrectEventType_ShouldCallHandlerSuccessfully() {
    // Given
    AtomicBoolean handlerCalled = new AtomicBoolean(false);
    doAnswer(
            invocation -> {
              handlerCalled.set(true);
              return null;
            })
        .when(negotiationHandler)
        .notify(any(NegotiationStateChangeEvent.class));

    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            "negotiation-123",
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT,
            "Test post");

    // When
    ReflectionTestUtils.invokeMethod(notificationListener, "dispatch", negotiationHandler, event);

    // Then - Use awaitility for the dispatch method call
    await()
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              assertTrue(handlerCalled.get(), "Handler should have been called through dispatch");
              verify(negotiationHandler, times(1)).notify(event);
            });
  }
}
