package eu.bbmri_eric.negotiator.negotiation.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.notification.email.EmailTemplateRepository;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailRepository;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class NegotiationPdfServiceImpl implements NegotiationPdfService {

    private TemplateEngine templateEngine;


    public NegotiationPdfServiceImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generatePdf(Negotiation negotiation, String templateName) throws Exception {
        Context context = new Context();
        context.setVariable("now", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy - h:mm a")));
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> payload = mapper.readValue(negotiation.getPayload(), new TypeReference<>() {});
        context.setVariable("negotiationPdfData", Map.of(
                "author", negotiation.getCreatedBy(),
                "id", negotiation.getId(),
//                "createdAt", negotiation.create,
                "status", negotiation.getCurrentState(),
                "payload", payload
        ));
        try {
            String renderedHtml = templateEngine.process(templateName, context);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(renderedHtml);
                renderer.layout();
                renderer.createPDF(outputStream);
                return outputStream.toByteArray();
            }
        }
        catch (Exception e) {
            throw new Exception("Error processing template: " + e.getMessage(), e);
        }


    }
}
