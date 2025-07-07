package eu.bbmri_eric.negotiator.notification;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;


public class NotificationModuleTest {


    @Test
    void name() {
        ApplicationModules modules = ApplicationModules.of("eu.bbmri_eric.negotiator.notification");
        modules.verify();
    }
}
