package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.UnreachableResourcesEvent;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnreachableResourcesHandlerTest {

  @Mock private EmailService emailService;

  @Mock private NegotiationRepository negotiationRepository;

  private UnreachableResourcesHandler handler;
  private long resourceIdCounter = 1000L; // Counter to ensure unique resource IDs

  @BeforeEach
  void setUp() {
    handler = new UnreachableResourcesHandler(emailService, negotiationRepository);
    resourceIdCounter = 1000L; // Reset counter for each test to ensure unique IDs
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    assertEquals(UnreachableResourcesEvent.class, handler.getSupportedEventType());
  }

  @Test
  void notify_WhenNegotiationNotFound_ThrowsEntityNotFoundException() {
    String negotiationId = "NEG-404";
    UnreachableResourcesEvent event = new UnreachableResourcesEvent(this, 1, negotiationId);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> handler.notify(event));
    verifyNoInteractions(emailService);
  }

  @Test
  void notify_WhenResourcesHaveRepresentatives_DoesNotSendEmails() {
    String negotiationId = "NEG-123";
    UnreachableResourcesEvent event = new UnreachableResourcesEvent(this, 1, negotiationId);

    Person representative = createPerson(1L, "Rep User", "rep@example.com");
    Set<Person> representatives = Set.of(representative);

    Resource resourceWithRep =
        createResourceWithRepresentatives(
            "resource-1", "Resource-1", "contact@org.com", representatives);
    Negotiation negotiation = createNegotiation(negotiationId, Set.of(resourceWithRep));

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    verifyNoInteractions(emailService);
  }

  @Test
  void notify_WhenResourcesHaveNoContactEmail_DoesNotSendEmails() {
    String negotiationId = "NEG-123";
    UnreachableResourcesEvent event = new UnreachableResourcesEvent(this, 2, negotiationId);

    Resource resourceWithoutContact =
        createResourceWithRepresentatives("resource-1", "Resource-1", null, new HashSet<>());
    Resource resourceWithEmptyContact =
        createResourceWithRepresentatives("resource-2", "Resource-2", "", new HashSet<>());
    Negotiation negotiation =
        createNegotiation(negotiationId, Set.of(resourceWithoutContact, resourceWithEmptyContact));

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    verifyNoInteractions(emailService);
  }

  @Test
  void notify_WhenSingleResourceNeedsNotification_SendsSingleEmail() {
    String negotiationId = "NEG-123";
    UnreachableResourcesEvent event = new UnreachableResourcesEvent(this, 1, negotiationId);

    Resource resource =
        createResourceWithRepresentatives(
            "resource-1", "Resource-1", "contact@org.com", new HashSet<>());
    Negotiation negotiation = createNegotiation(negotiationId, Set.of(resource));

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(emailService, times(1))
        .sendEmail(emailCaptor.capture(), titleCaptor.capture(), messageCaptor.capture());

    assertEquals("contact@org.com", emailCaptor.getValue());
    assertEquals("New Negotiation Request in the BBMRI-ERIC Negotiator", titleCaptor.getValue());
    assertTrue(messageCaptor.getValue().contains("Resource-1"));
    assertTrue(messageCaptor.getValue().contains("1 resource"));
  }

  @Test
  void notify_WhenMultipleResourcesSameOrganization_SendsSingleGroupedEmail() {
    String negotiationId = "NEG-123";
    UnreachableResourcesEvent event = new UnreachableResourcesEvent(this, 3, negotiationId);

    String sharedContactEmail = "contact@org.com";
    Resource resource1 =
        createResourceWithRepresentatives(
            "resource-1", "Resource-1", sharedContactEmail, new HashSet<>());
    Resource resource2 =
        createResourceWithRepresentatives(
            "resource-2", "Resource-2", sharedContactEmail, new HashSet<>());
    Resource resource3 =
        createResourceWithRepresentatives(
            "resource-3", "Resource-3", sharedContactEmail, new HashSet<>());

    Negotiation negotiation =
        createNegotiation(negotiationId, Set.of(resource1, resource2, resource3));

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(emailService, times(1))
        .sendEmail(emailCaptor.capture(), any(String.class), messageCaptor.capture());

    assertEquals(sharedContactEmail, emailCaptor.getValue());
    String message = messageCaptor.getValue();
    assertTrue(message.contains("3 resources"));
    assertTrue(message.contains("Resource-1"));
    assertTrue(message.contains("Resource-2"));
    assertTrue(message.contains("Resource-3"));
  }

  @Test
  void notify_WhenMultipleResourcesDifferentOrganizations_SendsSeparateEmails() {
    String negotiationId = "NEG-123";
    UnreachableResourcesEvent event = new UnreachableResourcesEvent(this, 3, negotiationId);

    Resource resource1 =
        createResourceWithRepresentatives(
            "resource-1", "Resource-1", "contact1@org1.com", new HashSet<>());
    Resource resource2 =
        createResourceWithRepresentatives(
            "resource-2", "Resource-2", "contact2@org2.com", new HashSet<>());
    Resource resource3 =
        createResourceWithRepresentatives(
            "resource-3", "Resource-3", "contact1@org1.com", new HashSet<>());

    Negotiation negotiation =
        createNegotiation(negotiationId, Set.of(resource1, resource2, resource3));

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(emailService, times(2))
        .sendEmail(emailCaptor.capture(), any(String.class), messageCaptor.capture());

    var capturedEmails = emailCaptor.getAllValues();
    var capturedMessages = messageCaptor.getAllValues();

    assertTrue(capturedEmails.contains("contact1@org1.com"));
    assertTrue(capturedEmails.contains("contact2@org2.com"));

    // One organization should get email for 2 resources, another for 1 resource
    boolean foundTwoResources =
        capturedMessages.stream().anyMatch(msg -> msg.contains("2 resources"));
    boolean foundOneResource =
        capturedMessages.stream().anyMatch(msg -> msg.contains("1 resource"));

    assertTrue(foundTwoResources);
    assertTrue(foundOneResource);
  }

  @Test
  void notify_WhenMixedResourcesWithAndWithoutRepresentatives_OnlySendsToUnrepresented() {
    String negotiationId = "NEG-123";
    UnreachableResourcesEvent event = new UnreachableResourcesEvent(this, 2, negotiationId);

    Person representative = createPerson(1L, "Rep User", "rep@example.com");

    Resource resourceWithRep =
        createResourceWithRepresentatives(
            "resource-with-rep", "Resource-With-Rep", "contact1@org.com", Set.of(representative));
    Resource resourceWithoutRep1 =
        createResourceWithRepresentatives(
            "resource-no-rep-1", "Resource-No-Rep-1", "contact2@org.com", new HashSet<>());
    Resource resourceWithoutRep2 =
        createResourceWithRepresentatives(
            "resource-no-rep-2", "Resource-No-Rep-2", "contact2@org.com", new HashSet<>());

    Negotiation negotiation =
        createNegotiation(
            negotiationId, Set.of(resourceWithRep, resourceWithoutRep1, resourceWithoutRep2));

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(emailService, times(1))
        .sendEmail(emailCaptor.capture(), any(String.class), messageCaptor.capture());

    assertEquals("contact2@org.com", emailCaptor.getValue());
    String message = messageCaptor.getValue();
    assertTrue(message.contains("2 resources"));
    assertTrue(message.contains("Resource-No-Rep-1"));
    assertTrue(message.contains("Resource-No-Rep-2"));
    assertFalse(message.contains("Resource-With-Rep"));
  }

  @Test
  void notify_MessageContainsRequiredInformation() {
    String negotiationId = "NEG-123";
    UnreachableResourcesEvent event = new UnreachableResourcesEvent(this, 1, negotiationId);

    Resource resource =
        createResourceWithRepresentatives(
            "test-resource", "Test-Resource", "contact@org.com", new HashSet<>());
    Negotiation negotiation = createNegotiation(negotiationId, Set.of(resource));

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendEmail(any(String.class), any(String.class), messageCaptor.capture());

    String message = messageCaptor.getValue();

    // Check for required information in the message
    assertTrue(message.contains("Test-Resource"));
    assertTrue(message.contains(negotiation.getTitle()));
    assertTrue(message.contains(negotiationId));
    assertTrue(message.contains(negotiation.getCreatedBy().getName()));
    assertTrue(message.contains(negotiation.getCreatedBy().getEmail()));
    assertTrue(message.contains("BBMRI-ERIC Negotiator Team"));

    // Check for HTML formatting
    assertTrue(message.contains("<p>"));
    assertTrue(message.contains("</p>"));
    assertTrue(message.contains("<strong>"));
    assertTrue(message.contains("<ul"));
    assertTrue(message.contains("<li>"));
  }

  private Person createPerson(Long id, String name, String email) {
    return Person.builder().id(id).name(name).email(email).build();
  }

  private Resource createResourceWithRepresentatives(
      String id, String name, String contactEmail, Set<Person> representatives) {
    return Resource.builder()
        .id(resourceIdCounter++) // Use incrementing counter for unique IDs
        .name(name)
        .sourceId(id) // Set the sourceId to the provided id parameter for uniqueness
        .contactEmail(contactEmail)
        .representatives(representatives)
        .build();
  }

  private Negotiation createNegotiation(String id, Set<Resource> resources) {
    Person requester = createPerson(100L, "John Requester", "requester@example.com");

    Negotiation negotiation =
        Negotiation.builder().id(id).title("Test Negotiation Project").resources(resources).build();

    // Set the createdBy field using reflection since it's managed by JPA auditing
    negotiation.setCreatedBy(requester);

    return negotiation;
  }
}
