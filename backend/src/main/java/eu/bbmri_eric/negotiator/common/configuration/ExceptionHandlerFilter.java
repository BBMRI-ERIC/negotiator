package eu.bbmri_eric.negotiator.common.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@CommonsLog
@Component
public class ExceptionHandlerFilter extends OncePerRequestFilter {

  private final HandlerExceptionResolver resolver;

  public ExceptionHandlerFilter(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
    try {
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      log.error("Spring Security Filter Chain Exception:", e);
      resolver.resolveException(request, response, null, e);
    }
  }
}
