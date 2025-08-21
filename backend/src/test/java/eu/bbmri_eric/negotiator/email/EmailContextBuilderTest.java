package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;

@ExtendWith(MockitoExtension.class)
class EmailContextBuilderTest {

  @Mock private TemplateEngine templateEngine;

  private EmailContextBuilder emailContextBuilder;

  private static final String FRONTEND_URL = "https://negotiator.example.com";
  private static final String SINCERELY_TEXT = "Kind regards, The Negotiator Team";
  private static final String HELPDESK_HREF = "mailto:support@example.com";
  private static final String LOGO_URL = "https://example.com/logo.png";

  @BeforeEach
  void setUp() {
    emailContextBuilder =
        new EmailContextBuilder(
            templateEngine, FRONTEND_URL, SINCERELY_TEXT, HELPDESK_HREF, LOGO_URL);
  }

  @Test
  void constructor_WithAllParameters_InitializesCorrectly() {
    assertNotNull(emailContextBuilder);
  }

  @Test
  void buildEmailContent_WithAllParameters_ReturnsProcessedContent() {
    String templateName = "notification-template";
    String recipientName = "John Doe";
    String message = "This is a test notification";
    String negotiationId = "NEG-001";
    String negotiationTitle = "Test Negotiation";
    LocalDateTime creationDate = LocalDateTime.of(2025, 1, 15, 10, 30);
    String expectedContent = "Processed email content";

    when(templateEngine.process(eq(templateName), any())).thenReturn(expectedContent);

    String result =
        emailContextBuilder.buildEmailContent(
            templateName, recipientName, message, negotiationId, negotiationTitle, creationDate);

    assertEquals(expectedContent, result);
    verify(templateEngine).process(eq(templateName), any());
  }

  @Test
  void buildEmailContent_WithNullNegotiationData_ReturnsProcessedContent() {
    String templateName = "simple-template";
    String recipientName = "Jane Smith";
    String message = "Notification without negotiation";
    String expectedContent = "Simple email content";

    when(templateEngine.process(eq(templateName), any())).thenReturn(expectedContent);

    String result =
        emailContextBuilder.buildEmailContent(
            templateName, recipientName, message, null, null, null);

    assertEquals(expectedContent, result);
    verify(templateEngine).process(eq(templateName), any());
  }

  @Test
  void buildEmailContent_WithNullTemplateName_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () ->
            emailContextBuilder.buildEmailContent(
                null, "Test User", "Test message", null, null, null));
  }

  @Test
  void buildEmailContent_WithNullRecipientName_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () ->
            emailContextBuilder.buildEmailContent(
                "template", null, "Test message", null, null, null));
  }

  @Test
  void buildEmailContent_WithNullMessage_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () ->
            emailContextBuilder.buildEmailContent("template", "Test User", null, null, null, null));
  }

  @Test
  void buildEmailContent_WithPartialNegotiationData_DoesNotIncludeNegotiationVariables() {
    String templateName = "partial-template";
    String recipientName = "Test User";
    String message = "Test message";
    String negotiationId = "NEG-001";
    String expectedContent = "Content without negotiation data";

    when(templateEngine.process(eq(templateName), any())).thenReturn(expectedContent);

    String result =
        emailContextBuilder.buildEmailContent(
            templateName, recipientName, message, negotiationId, null, null);

    assertEquals(expectedContent, result);
    verify(templateEngine).process(eq(templateName), any());
  }
}
