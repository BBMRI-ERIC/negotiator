package eu.bbmri_eric.negotiator.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/** HTTP requests logger */
@Component
class HttpRequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

  // ANSI color codes
  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_RED = "\u001B[31m";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Instant startTime = Instant.now();
    logger.debug(
        "Request: {} {} from {}", request.getMethod(), getFullUri(request), getClientIp(request));

    try {
      filterChain.doFilter(request, response);
    } finally {
      Duration duration = Duration.between(startTime, Instant.now());
      logger.debug(
          "Response: {} {} -> {} ({}ms)",
          request.getMethod(),
          request.getRequestURI(),
          getColoredStatusCode(response.getStatus()),
          duration.toMillis());
    }
  }

  private String getFullUri(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String queryString = request.getQueryString();
    return queryString != null ? uri + "?" + queryString : uri;
  }

  private String getClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isEmpty()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String getColoredStatusCode(int statusCode) {
    if (statusCode >= 200 && statusCode < 300) {
      return ANSI_GREEN + statusCode + ANSI_RESET;
    } else if (statusCode >= 400) {
      return ANSI_RED + statusCode + ANSI_RESET;
    } else {
      return String.valueOf(statusCode);
    }
  }
}
