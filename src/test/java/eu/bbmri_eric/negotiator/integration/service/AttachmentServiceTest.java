package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.database.model.Attachment;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.views.MetadataAttachmentView;
import eu.bbmri_eric.negotiator.database.repository.AttachmentRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.database.view_repository.AttachmentViewRepository;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri_eric.negotiator.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.service.AttachmentService;
import eu.bbmri_eric.negotiator.service.NegotiationService;
import eu.bbmri_eric.negotiator.unit.context.WithMockNegotiatorUser;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AttachmentServiceTest {
  // TODO: test wrong when sending to an org part of the negotiation but from another representative
  // (BadRequest)

  private static final String ORG_1 = "org_1";
  private static final String ORG_2 = "org_2";
  private static final String NEGOTIATION_1_ID = "negotiation-1";
  private static final String NEGOTIATION_4_ID = "negotiation-4";
  private static final String RESOURCE_1 = "resource_1";
  private static final String RESOURCE_2 = "resource_2";
  private static final String REQUEST_1 = "request_1";
  private static final String REQUEST_2 = "request_2";
  @Autowired private AttachmentService attachmentService;
  @Autowired private NegotiationService negotiationService;
  @Autowired private DataSource dbSource;
  @Autowired private PersonRepository personRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private RequestRepository requestRepository;
  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private AttachmentViewRepository attachmentViewRepository;
  @Autowired private AttachmentRepository attachmentRepository;

  public void addH2Function() {
    String statementScript =
        "create DOMAIN IF NOT EXISTS JSONB AS JSON; \n"
            + "CREATE ALIAS IF NOT EXISTS JSONB_EXTRACT_PATH AS '\n"
            + "import com.jayway.jsonpath.JsonPath;\n"
            + "    @CODE\n"
            + "    String jsonbExtractPath(String jsonString, String...jsonPaths) {\n"
            + "      String overallPath = String.join(\".\", jsonPaths);\n"
            + "      try {\n"
            + "        Object result = JsonPath.read(jsonString, overallPath);\n"
            + "        if (result != null) {\n"
            + "          return result.toString();\n"
            + "        }\n"
            + "      } catch (Exception e) {\n"
            + "        e.printStackTrace();\n"
            + "      }\n"
            + "      return null;\n"
            + "    }';";
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dbSource);
    jdbcTemplate.execute(statementScript);
  }

  @BeforeEach
  void setUp() {
    addH2Function();
  }

  private Attachment createAttachment(
      Organization organization, Negotiation negotiation, Person creator) {
    Attachment attachment =
        Attachment.builder()
            .id("abcdef")
            .size(100L)
            .name("Attachment")
            .contentType("application/pdf")
            .payload(new byte[] {1, 1})
            .negotiation(negotiation)
            .organization(organization)
            .build();
    attachment.setCreatedBy(creator);
    return attachmentRepository.save(attachment);
  }

  /**
   * Test that an attachment is created correctly for a negotiation when the creator is the creator
   * of the Negotiation
   */
  @Test
  @WithMockNegotiatorUser(id = 108L)
  public void testCreate_ByCreator_success_when_negotiationCreator() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    AttachmentMetadataDTO attachment =
        attachmentService.createForNegotiation(NEGOTIATION_1_ID, null, mockMultipartFile);
    Attachment created = attachmentRepository.findById(attachment.getId()).orElse(null);
    assertNotNull(created);
    assertEquals(created.getNegotiation().getId(), "negotiation-1");
    assertEquals(created.getCreatedBy().getId(), 108L);
  }

  /**
   * Test that an attachment is created correctly for a negotiation when the creator is the
   * representative of a resource involved in the Negotiation
   */
  @Test
  @WithMockNegotiatorUser(id = 109L)
  public void testCreate_ByRepresentative_success() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    AttachmentMetadataDTO attachment =
        attachmentService.createForNegotiation(NEGOTIATION_1_ID, null, mockMultipartFile);
    Attachment created = attachmentRepository.findById(attachment.getId()).orElse(null);
    assertNotNull(created);
    assertEquals(created.getNegotiation().getId(), "negotiation-1");
    assertEquals(created.getCreatedBy().getId(), 109L);
  }

  /**
   * Test that an attachment is created correctly for a negotiation when the creator is the
   * representative of a resource involved in the Negotiation
   */
  @Test
  @WithMockNegotiatorUser(id = 104L)
  public void testCreate_ByRepresentative_forbidden_when_RepresentativeNotPartOfTheOrganization() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    assertThrows(
        ForbiddenRequestException.class,
        () -> attachmentService.createForNegotiation(NEGOTIATION_1_ID, null, mockMultipartFile));
  }

  /**
   * Tests creation of an attachment for a negotiation and a specific organization part of the
   * negotiation by the negotiation creator
   */
  @Test
  @WithMockNegotiatorUser(id = 108L)
  public void test_ByCreator_ToAnOrganization_ok() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    AttachmentMetadataDTO attachment =
        attachmentService.createForNegotiation(NEGOTIATION_1_ID, "biobank:1", mockMultipartFile);
    // using the view repository for convenienc, since it laod the LAZY loadad organization
    MetadataAttachmentView created =
        attachmentViewRepository.findById(attachment.getId()).orElse(null);
    assertNotNull(created);
    assertEquals(created.getNegotiation().getId(), "negotiation-1");
    assertEquals(created.getCreatedBy().getId(), 108L);
    assertEquals(created.getOrganization().getExternalId(), "biobank:1");
  }

  /**
   * Tests creation of an attachment for a negotiation and sent to a specific organization part of
   * the negotiation by the organization representative
   */
  @Test
  @WithMockNegotiatorUser(id = 109L)
  public void test_ByRepresentative_ToOrganization_ok() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    AttachmentMetadataDTO attachment =
        attachmentService.createForNegotiation(NEGOTIATION_1_ID, "biobank:1", mockMultipartFile);
    // using the view repository for convenienc, since it laod the LAZY loadad organization
    MetadataAttachmentView created =
        attachmentViewRepository.findById(attachment.getId()).orElse(null);
    assertNotNull(created);
    assertEquals(created.getNegotiation().getId(), "negotiation-1");
    assertEquals(created.getCreatedBy().getId(), 109L);
    assertEquals(created.getOrganization().getExternalId(), "biobank:1");
  }

  /**
   * Tests raises WrongRequest creating an attachment for a negotiation and sent to a specific
   * organization that is not involved part of in the negotiation
   */
  @Test
  @WithMockNegotiatorUser(id = 108L)
  public void test_ByCreator_ToAnOrganization_fails_when_organizationNotPartOfTheNegoatiation() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    assertThrows(
        WrongRequestException.class,
        () ->
            attachmentService.createForNegotiation(
                NEGOTIATION_1_ID, "biobank:2", mockMultipartFile));
  }

  /**
   * Tests WrongException creating an attachment for a negotiation and sent to a specific
   * organization by organization's representative when the organization is not part of the
   * negotiation
   */
  @Test
  @WithMockNegotiatorUser(id = 109L)
  public void
      testCreate_ByRepresentative_ToOrganization_fails_when_organizationNotPartOfNegotiation() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    assertThrows(
        WrongRequestException.class,
        () ->
            attachmentService.createForNegotiation(
                NEGOTIATION_1_ID, "biobank:2", mockMultipartFile));
  }

  /**
   * Tests ForbiddenRequest creating an attachment for a negotiation and sent to a specific
   * organization by representative of another organization in the negotiation
   */
  @Test
  @WithMockNegotiatorUser(id = 109L) // 109 is the Biobanker and represents biobank 1 and 2 not 3
  public void
      testCreate_ByRepresentative_ToOrganization_fails_when_noRepresentativeOfTheOrganization() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    assertThrows(
        ForbiddenRequestException.class,
        () ->
            attachmentService.createForNegotiation(
                NEGOTIATION_4_ID, "biobank:3", mockMultipartFile));
  }
}
