package eu.bbmri_eric.negotiator.negotiation.pdf;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Routes errors logged by iText through the PDF service logger, so they show up as part of the
 * negotiator's own logs instead of as internal library output.
 */
@Component
class ITextLogForwarder {
  private static final String ITEXT_LOGGER = "com.itextpdf";
  private static final Log log = LogFactory.getLog(NegotiationPdfServiceImpl.class);

  @PostConstruct
  void install() {
    if (!(LoggerFactory.getLogger(ITEXT_LOGGER) instanceof Logger iTextLogger)) {
      return;
    }
    AppenderBase<ILoggingEvent> appender =
        new AppenderBase<>() {
          @Override
          protected void append(ILoggingEvent event) {
            forward(event);
          }
        };
    appender.setContext(iTextLogger.getLoggerContext());
    appender.setName("itext-forwarder");
    appender.start();
    iTextLogger.detachAppender(appender.getName());
    iTextLogger.addAppender(appender);
    iTextLogger.setAdditive(false);
  }

  private static void forward(ILoggingEvent event) {
    String loggerName = event.getLoggerName();
    String message =
        "iText (%s): %s"
            .formatted(
                loggerName.substring(loggerName.lastIndexOf('.') + 1), event.getFormattedMessage());
    Throwable throwable = toThrowable(event.getThrowableProxy());
    if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
      log.error(message, throwable);
    }
  }

  private static Throwable toThrowable(IThrowableProxy proxy) {
    return proxy instanceof ThrowableProxy throwableProxy ? throwableProxy.getThrowable() : null;
  }
}
