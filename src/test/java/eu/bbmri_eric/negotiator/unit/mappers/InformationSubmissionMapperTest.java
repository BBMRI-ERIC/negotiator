package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.info_requirement.InformationSubmission;
import eu.bbmri_eric.negotiator.info_requirement.InformationSubmissionMapper;
import eu.bbmri_eric.negotiator.info_requirement.SubmittedInformationDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class InformationSubmissionMapperTest {
  private ModelMapper modelMapper;

  @BeforeEach
  public void setUp() {
    modelMapper = new ModelMapper();
    InformationSubmissionMapper informationSubmissionMapper =
        new InformationSubmissionMapper(modelMapper);
    informationSubmissionMapper.addMappings();
  }

  @Test
  void testMappings_normalJson_ok() throws IOException {
    String content =
        """
                              {
                             "sample-type": "DNA",
                             "num-of-subjects": 10,
                             "num-of-samples": 20,
                             "volume-per-sample": 5
                          }
                          """;
    JsonNode payload = new ObjectMapper().readTree(content);
    InformationRequirement requirement =
        new InformationRequirement(1L, new AccessForm("test"), NegotiationResourceEvent.CONTACT);
    InformationSubmission entity = new InformationSubmission(requirement, null, null, content);
    assertEquals(payload, modelMapper.map(entity, SubmittedInformationDTO.class).getPayload());
  }

  @Test
  void testMappings_nullJson_emptyJson() throws IOException {
    InformationRequirement requirement =
        new InformationRequirement(1L, new AccessForm("test"), NegotiationResourceEvent.CONTACT);
    InformationSubmission entity = new InformationSubmission(requirement, null, null, null);
    assertEquals(
        new ObjectMapper().readTree("{}"),
        modelMapper.map(entity, SubmittedInformationDTO.class).getPayload());
  }

  @Test
  void testMappings_nullArguments_equalsNull() {
    InformationRequirement requirement =
        new InformationRequirement(1L, new AccessForm("test"), NegotiationResourceEvent.CONTACT);
    InformationSubmission entity = new InformationSubmission(requirement, null, null, null);
    assertEquals(null, modelMapper.map(entity, SubmittedInformationDTO.class).getResourceId());
    assertEquals(null, modelMapper.map(entity, SubmittedInformationDTO.class).getId());
  }
}
