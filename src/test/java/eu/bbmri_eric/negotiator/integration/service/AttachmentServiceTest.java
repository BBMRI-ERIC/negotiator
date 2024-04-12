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
import eu.bbmri_eric.negotiator.database.view_repository.AttachmentViewRepository;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.service.AttachmentService;
import eu.bbmri_eric.negotiator.unit.context.WithMockNegotiatorUser;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests creation and retrieval of attachments using the DBAttachments service The scenario for
 * testing attachments retrieval contains 5 attachments for one negotiation. The negotiation has two
 * resource from two different organizations. There are 3 users involved: * - 108 (TheResearcher):
 * creator of the negotiation * - 105 (SarahRepr): representative of biobank:3 * - 109
 * (TheBiobanker): representative of biobank:1 The attachments are - 2 public (i.e., no organization
 * is specified) - 2 private for biobank:1, one from TheResearcher, one from TheBiobanker - 1
 * private for biobank:2 from TheResearcer
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AttachmentServiceTest {

  // Negotiation creted by 109 with one resource and one organization biobank:1 represented by 108
  private static final String NEGOTIATION_1_ID = "negotiation-1";
  // Negotiation creted by 109 with two resources: one of organization biobank:1 represented by 108
  // and one of organization biobank:2 represented by 105
  private static final String NEGOTIATION_2_ID = "negotiation-2";
  private static final String ATTACHMENT_1_ID = "attachment-1";
  private static final String ATTACHMENT_2_ID = "attachment-2";

  @Autowired private AttachmentService attachmentService;
  @Autowired private DataSource dbSource;
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
  public void testCreateForNegotiation_ByCreator_success_when_negotiationCreator() {
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
  public void testCreateForNegotiation_ByRepresentative_success() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    AttachmentMetadataDTO attachment =
        attachmentService.createForNegotiation(NEGOTIATION_1_ID, null, mockMultipartFile);

    Attachment created = attachmentRepository.findById(attachment.getId()).orElse(null);
    assertNotNull(created);
    assertEquals(created.getNegotiation().getId(), "negotiation-1");
    assertEquals(created.getCreatedBy().getId(), 109L);
  }

  /** Test that the creation fails when the user is not part of the negotiation */
  @Test
  @WithMockNegotiatorUser(id = 104L)
  public void
      testCreateForNegotiation_ByRepresentative_forbidden_when_RepresentativeNotPartOfTheOrganization() {
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
  public void testCreateForNegotiation_ByCreator_ToAnOrganization_ok() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    AttachmentMetadataDTO attachment =
        attachmentService.createForNegotiation(NEGOTIATION_1_ID, "biobank:1", mockMultipartFile);
    // using the view repository for convenience, since it laod the LAZY loadad organization
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
  public void testCreateForNegotiation_ByRepresentative_ToOrganization_ok() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    AttachmentMetadataDTO attachment =
        attachmentService.createForNegotiation(NEGOTIATION_1_ID, "biobank:1", mockMultipartFile);
    // using the view repository for convenienc, since it laod the otherwise LAZY loadad
    // organization
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
  public void
      testCreateForNegotiation_ByCreator_ToAnOrganization_fails_when_organizationNotPartOfTheNegoatiation() {
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
      testCreateForNegotiation_ByRepresentative_ToOrganization_fails_when_organizationNotPartOfNegotiation() {
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
      testCreateForNegotiation_ByRepresentative_ToOrganization_fails_when_RepresentativeOfTheOrganization() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    assertThrows(
        ForbiddenRequestException.class,
        () ->
            attachmentService.createForNegotiation(
                NEGOTIATION_2_ID, "biobank:3", mockMultipartFile));
  }

  /** Tests create successfully when not specifying a negotiation */
  @Test
  @WithMockNegotiatorUser(id = 108L)
  public void testCreate() {
    byte[] inputArray = "Test String".getBytes();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);
    AttachmentMetadataDTO attachment = attachmentService.create(mockMultipartFile);
    MetadataAttachmentView created =
        attachmentViewRepository.findById(attachment.getId()).orElse(null);
    assertNotNull(created);
  }

  /**
   * Tests getting list of attachments metadata for a negotiation by the creator of the negotiation.
   * Returns all the attachments of the negotiation
   */
  @Test
  @WithMockNegotiatorUser(id = 108L)
  public void testFindMetadata_ByNegotiationId_byCreator() {
    List<AttachmentMetadataDTO> attachments = attachmentService.findByNegotiation(NEGOTIATION_2_ID);
    assertEquals(attachments.size(), 5);
  }

  /**
   * Tests getting list of attachments metadata for a negotiation by the creator of the negotiation.
   * Returns all the attachments of the negotiation
   */
  @Test
  @WithMockNegotiatorUser(id = 108L)
  public void testFindMetadata_ByNegotiationIdAndAttachmentId_byCreator() {
    AttachmentMetadataDTO attachment =
        attachmentService.findByIdAndNegotiationId(ATTACHMENT_1_ID, NEGOTIATION_2_ID);
    assertEquals(attachment.getId(), ATTACHMENT_1_ID);
  }

  /**
   * Tests that trying to access attachments of an unknown negotiation raise an
   * EntityNotFoundException
   */
  @Test
  @WithMockNegotiatorUser(id = 108L)
  public void testFindMetadata_ByNegotiationId_fails_whenNegotiationNotExists() {
    assertThrows(EntityNotFoundException.class, () -> attachmentService.findByNegotiation("UNKN"));
  }

  /**
   * Tests getting list of attachments metadata for a negotiation by the representative of a
   * resource in the negotiation. Returns public attachments and the ones sent to the organization
   * of the representative
   */
  @Test
  @WithMockNegotiatorUser(id = 109L)
  public void testFindMetadata_ByNegotiationId_byRepresentative_ok() {
    List<AttachmentMetadataDTO> attachments = attachmentService.findByNegotiation(NEGOTIATION_2_ID);
    assertEquals(attachments.size(), 4);
  }

  /**
   * Tests getting list of attachments metadata for a negotiation by the representative of a
   * resource in the negotiation. Returns public attachments and the ones sent to the organization
   * of the representative
   */
  @Test
  @WithMockNegotiatorUser(id = 109L)
  public void testFindMetadata_ByNegotiationIdAndAttachment_byRepresentative_ok_whenPublic() {
    AttachmentMetadataDTO attachment =
        attachmentService.findByIdAndNegotiationId(ATTACHMENT_1_ID, NEGOTIATION_2_ID);
    assertEquals(attachment.getId(), ATTACHMENT_1_ID);
  }

  /**
   * Tests getting a specific attachment for by a representative for his/her own organization. The
   * retrieval is ok
   */
  @Test
  @WithMockNegotiatorUser(id = 109L)
  public void
      testFindMetadata_ByNegotiationIdAndAttachment_byRepresentative_ok_whenPrivateForOrganization() {
    AttachmentMetadataDTO attachment =
        attachmentService.findByIdAndNegotiationId(ATTACHMENT_1_ID, NEGOTIATION_2_ID);
    assertEquals(attachment.getId(), ATTACHMENT_1_ID);
  }

  /** Tests getting a specific attachment for by a representative for his/her own organization */
  @Test
  @WithMockNegotiatorUser(id = 105L)
  public void
      testFindMetadata_ByNegotiationIdAndAttachment_byRepresentative_fails_whenPrivateForOrganization() {
    assertThrows(
        ForbiddenRequestException.class,
        () -> attachmentService.findByIdAndNegotiationId(ATTACHMENT_2_ID, NEGOTIATION_2_ID));
  }
}
