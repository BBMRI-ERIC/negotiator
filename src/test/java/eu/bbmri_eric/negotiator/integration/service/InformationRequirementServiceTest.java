package eu.bbmri_eric.negotiator.integration.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class InformationRequirementServiceTest {
  @Test
  void createInformationRequirement_passedNull_throwsNullPointerException() {}
}
