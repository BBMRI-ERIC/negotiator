package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
  private static final String PROBLEM_JSON_CONTENT_TYPE = "application/problem+json";
  private static final URI FORBIDDEN_ERROR_URI =
      URI.create("https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/403");

  private final ObjectMapper objectMapper;

  public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, accessDeniedException.getMessage());
    problemDetail.setTitle("Forbidden");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setType(FORBIDDEN_ERROR_URI);
    problemDetail.setProperties(Map.of());

    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(PROBLEM_JSON_CONTENT_TYPE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    response.getOutputStream().write(objectMapper.writeValueAsBytes(problemDetail));
  }
}
