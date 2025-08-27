package eu.bbmri_eric.negotiator.negotiation.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.template.TemplateService;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@CommonsLog
public class PdfContextBuilder {
    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy - h:mm a 'UTC'");
    private final ObjectMapper objectMapper;
    private final TemplateService templateService;

    @Value("${negotiator.emailLogo}")
    private String logoURL;

    public PdfContextBuilder(ObjectMapper objectMapper, TemplateService templateService) {
        this.objectMapper = objectMapper;
        this.templateService = templateService;
    }

    @Transactional
    public String createPdfContent(Negotiation negotiation, String templateName) {
        try {
            Map<String, Object> payload =
                    this.objectMapper.readValue(negotiation.getPayload(), new TypeReference<>() {
                    });
            // DO NOT remove any variables without a good reason.
            // Any deployments without updated templates would miss them.
            // If you add or modify them mention it in the documentation.
            Map<String, Object> variables = new HashMap<>();
            variables.put("now", LocalDateTime.now().format(DTF));
            variables.put("logoUrl", logoURL);
            variables.put("authorName", negotiation.getCreatedBy().getName());
            variables.put("authorEmail", negotiation.getCreatedBy().getEmail());
            variables.put("negotiationId", negotiation.getId());
            variables.put("negotiationTitle", negotiation.getTitle());
            variables.put("negotiationCreatedAt", negotiation.getCreationDate().format(DTF));
            variables.put("negotiationStatus", negotiation.getCurrentState());
            variables.put("negotiationPayload", processPayload(payload));
            variables.put("resourcesByOrganization", getResourcesByOrganization(negotiation));
            variables.put("totalResourceCount", negotiation.getResources().size());
            variables.put("totalOrganizationCount", negotiation.getOrganizations().size());

            return templateService.processTemplate(variables, templateName);
        } catch (Exception e) {
            log.error("Failed to create PDF content for negotiation " + negotiation.getId(), e);
            return "";
        }
    }

    /**
     * Groups resources by their organization for display in the PDF.
     *
     * @param negotiation the negotiation containing resources
     * @return Map where keys are organization names and values are lists of resource names
     */
    private Map<String, List<String>> getResourcesByOrganization(Negotiation negotiation) {
        Set<Resource> resources = negotiation.getResources();

        return resources.stream()
                .collect(
                        Collectors.groupingBy(
                                resource -> resource.getOrganization().getName(),
                                LinkedHashMap::new, // Preserve insertion order
                                Collectors.mapping(Resource::getName, Collectors.toList())));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processPayload(Map<String, Object> payload) {
        payload.replaceAll(
                (key, value) -> {
                    if (value instanceof String str) {
                        return escapeHtml(str).replaceAll("(<br />)+$", "");
                    } else if (value instanceof Map<?, ?> map) {
                        if (map.isEmpty()) {
                            return "Empty";
                        }
                        return processPayload((Map<String, Object>) map);
                    } else if (value instanceof Iterable<?> iterable) {
                        return StreamSupport.stream(iterable.spliterator(), false)
                                .map(item -> (item instanceof String s) ? escapeHtml(s) : item)
                                .collect(Collectors.toList());
                    }
                    return value;
                });
        return payload;
    }

    private String escapeHtml(String input) {
        String escapedText = HtmlUtils.htmlEscape(input);

        return escapedText.replace("\n", "<br />");
    }
}
