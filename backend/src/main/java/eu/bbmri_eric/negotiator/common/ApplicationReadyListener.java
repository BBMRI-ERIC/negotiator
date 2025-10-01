package eu.bbmri_eric.negotiator.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** Start up message printer */
@Component
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_RESET = "\u001B[0m";

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    List<String> messages =
        new ArrayList<>(
            List.of(
                "🛩️ Red Five standing by. All systems go.",
                "🚀 Hyperdrive engaged. Punch it, Chewie!",
                "🎉 App started. This is where the fun begins.",
                "✨ The Force is with us. App startup complete.",
                "🌠 App ready. Preparing to jump to hyperspace."));
    Collections.shuffle(messages);
    System.out.println(ANSI_GREEN + messages.getFirst() + ANSI_RESET);
  }
}
