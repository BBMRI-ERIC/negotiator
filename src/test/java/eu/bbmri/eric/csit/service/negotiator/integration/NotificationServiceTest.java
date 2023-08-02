package eu.bbmri.eric.csit.service.negotiator.integration;


import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class NotificationServiceTest {}
