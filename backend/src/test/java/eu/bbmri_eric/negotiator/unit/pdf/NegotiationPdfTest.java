package eu.bbmri_eric.negotiator.unit.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.common.exceptions.PdfGenerationException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.pdf.NegotiationPdfServiceImpl;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.user.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NegotiationPdfTest {
    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NegotiationPdfServiceImpl negotiationPdfService;

    private Negotiation negotiation;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(negotiationPdfService, "fontPath", "/fonts/OpenSans.ttf");
        ReflectionTestUtils.setField(negotiationPdfService, "logoURL", "http://localhost:8081/api/images/negotiator_logo.png");

        negotiation = new Negotiation();
        negotiation.setId("test-id");
        negotiation.setCreationDate(LocalDateTime.now());
        negotiation.setCreatedBy(new Person());
        negotiation.setPayload("{\"sample\":\"data\"}");
        negotiation.setCurrentState(NegotiationState.IN_PROGRESS);

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("sample", "data");

        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(payloadMap);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html><body><h1>Test</h1></body></html>");
    }

    @Test
    void createNegotiationPdf() {
        byte[] pdfBytes = negotiationPdfService.generatePdf(negotiation, "pdf-negotiation-summary");

        assertNotNull(pdfBytes);
    }

    @Test
    void missingFontURL() {
        ReflectionTestUtils.setField(negotiationPdfService, "fontPath", "/fonts/Arial.ttf");

        assertThrows(
                PdfGenerationException.class,
                () -> {
                    negotiationPdfService.generatePdf(negotiation, "pdf-negotiation-summary");
                });
    }
}
