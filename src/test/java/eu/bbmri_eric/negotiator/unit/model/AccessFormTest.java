package eu.bbmri_eric.negotiator.unit.model;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import org.junit.jupiter.api.Test;

public class AccessFormTest {

  @Test
  void initAccessForm_noArgsConstructor_ok() {
    new AccessForm("test");
  }
  
}
