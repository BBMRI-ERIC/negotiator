package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.bbmri_eric.negotiator.attachment.Attachment;
import eu.bbmri_eric.negotiator.attachment.AttachmentRepository;
import eu.bbmri_eric.negotiator.attachment.AttachmentViewDTO;
import eu.bbmri_eric.negotiator.attachment.MetadataAttachmentViewDTO;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryTest
public class AttachmentRepositoriesTest {
  private static final String ORG_1 = "org_1";
  private static final String ORG_2 = "org_2";
  private static final String NEGOTIATION_1_ID = "negotiation_1";
  private static final String NEGOTIATION_2_ID = "negotiation_2";
  private static final String RESOURCE_1 = "resource_1";
  private static final String RESOURCE_2 = "resource_2";
  private static final String REQUEST_1 = "request_1";
  private static final String REQUEST_2 = "request_2";

  @Autowired private PersonRepository personRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private RequestRepository requestRepository;
  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private AttachmentRepository attachmentRepository;

  private DiscoveryService discoveryService;
  private Person person;

  private Resource resource1;
  private Resource resource2;
  private Request request1;
  private Request request2;
  private Negotiation negotiation1;
  private Negotiation negotiation2;
  private Organization organization1;
  private Organization organization2;
  
  @BeforeEach
  void setUp() {
    this.discoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());
    this.person = createPerson("person1");
    this.organization1 = createOrganization(ORG_1);
    this.organization2 = createOrganization(ORG_2);
    this.resource1 = createResource(this.organization1, RESOURCE_1);
    this.resource2 = createResource(this.organization2, RESOURCE_2);
    this.request1 = createRequest(resource1, REQUEST_1);
    this.request2 = createRequest(resource2, REQUEST_2);
    this.negotiation1 = createNegotiation(NEGOTIATION_1_ID, request1);
    this.negotiation2 = createNegotiation(NEGOTIATION_2_ID, request2);
  }

  private Organization createOrganization(String organizationID) {
    return organizationRepository.save(
        Organization.builder()
            .name(organizationID)
            .externalId("EXT_%s".formatted(organizationID))
            .build());
  }

  private Resource createResource(Organization organization, String resourceId) {
    return resourceRepository.save(
        Resource.builder()
            .organization(organization)
            .discoveryService(discoveryService)
            .sourceId(resourceId)
            .name("resource")
            .representatives(new HashSet<>(List.of(person)))
            .build());
  }

  private Request createRequest(Resource resource, String requestId) {
    return requestRepository.save(
        Request.builder()
            .id(requestId)
            .url("http://test")
            .resources(Set.of(resource))
            .discoveryService(discoveryService)
            .humanReadable("everything")
            .build());
  }

  private Negotiation createNegotiation(String negotiationId, Request request) {
    return negotiationRepository.save(
        Negotiation.builder()
            .id(negotiationId)
            .requests(Set.of(request))
            .payload("{\"project\":{\"title\":\"negtitle\"} }")
            .build());
  }

  private Person createPerson(String subjectId) {
    return personRepository.save(
        Person.builder()
            .subjectId(subjectId)
            .name("John")
            .email("test@test.com")
            .resources(new HashSet<>())
            .build());
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

  /** Tests get by negotiation id. Out of 3 attachment 2 are returned */
  @Test
  void findByNegotiationId_ok() {
    Person creator = createPerson("pers1");
    Attachment attachment1 = createAttachment(organization1, negotiation1, creator);
    Attachment attachment2 = createAttachment(null, negotiation1, creator);
    createAttachment(organization2, negotiation2, creator);

    List<MetadataAttachmentViewDTO> attachmentsViews =
        attachmentRepository.findByNegotiationId(this.negotiation1.getId());

    assertEquals(2, attachmentsViews.size());

    assertEquals(attachmentsViews.get(0).getId(), attachment1.getId());
    assertEquals(attachmentsViews.get(1).getId(), attachment2.getId());

    assertEquals(attachmentsViews.get(0).getSize(), attachment1.getSize());
    assertEquals(attachmentsViews.get(0).getContentType(), attachment1.getContentType());
    assertEquals(attachmentsViews.get(0).getName(), attachment1.getName());
    assertEquals(attachmentsViews.get(0).getNegotiationId(), this.negotiation1.getId());
    assertEquals(
        attachmentsViews.get(0).getOrganizationExternalId(), organization1.getExternalId());
    assertEquals(attachmentsViews.get(0).getCreatedById(), attachment1.getCreatedBy().getId());
  }

  /** Tests get by negotiation id with empty results */
  @Test
  void findByNegotiationId_empty() {
    Person creator = createPerson("pers1");
    createAttachment(organization1, negotiation1, creator);
    createAttachment(null, negotiation1, creator);

    List<MetadataAttachmentViewDTO> attachmentsViews =
        attachmentRepository.findByNegotiationId(negotiation2.getId());

    assertEquals(0, attachmentsViews.size());
  }

  /**
   * Tests retrieval by both attachment id and negotiation ok. Out of 3 attachments only one is
   * returned
   */
  @Test
  void findMetadataByIdAndNegotiationId_ok() {
    Person creator = createPerson("pers1");
    Attachment attachment1 = createAttachment(organization1, negotiation1, creator);
    createAttachment(organization2, negotiation1, creator);
    createAttachment(organization2, negotiation2, creator);

    MetadataAttachmentViewDTO attachmentView =
        attachmentRepository
            .findMetadataByIdAndNegotiationId(attachment1.getId(), this.negotiation1.getId())
            .orElse(null);

    assertNotNull(attachmentView);
    assertEquals(attachmentView.getId(), attachment1.getId());
    assertEquals(attachmentView.getSize(), attachment1.getSize());
    assertEquals(attachmentView.getContentType(), attachment1.getContentType());
    assertEquals(attachmentView.getName(), attachment1.getName());
    assertEquals(attachmentView.getNegotiationId(), this.negotiation1.getId());
    assertEquals(attachmentView.getOrganizationExternalId(), organization1.getExternalId());
    assertEquals(attachmentView.getCreatedById(), attachment1.getCreatedBy().getId());
  }

  /**
   * Tests that trying to get an attachment by correct id but wrong negotiation id, it doesn't
   * return an attachment
   */
  @Test
  void findMetadataByIdAndNegotiationId_notFound_whenAttachmentExistuButNotPartOfNegotiation() {
    Person creator = createPerson("pers1");
    createAttachment(organization1, negotiation1, creator);
    createAttachment(organization2, negotiation1, creator);
    Attachment attachment3 = createAttachment(organization2, negotiation2, creator);

    MetadataAttachmentViewDTO attachmentView =
        attachmentRepository
            .findMetadataByIdAndNegotiationId(attachment3.getId(), this.negotiation1.getId())
            .orElse(null);

    assertNull(attachmentView);
  }

  /**
   * Tests that trying to get an attachment by correct id but wrong negotiation id, it doesn't
   * return an attachment
   */
  @Test
  void findMetadataByIdAndNegotiationId_notFound_whenNegotiationIsEmpty() {
    Person creator = createPerson("pers1");
    Attachment attachment3 = createAttachment(organization2, negotiation2, creator);

    MetadataAttachmentViewDTO attachmentView =
        attachmentRepository
            .findMetadataByIdAndNegotiationId(attachment3.getId(), this.negotiation1.getId())
            .orElse(null);

    assertNull(attachmentView);
  }

  @Test
  void getPayloadById_ok() {
    Person creator = createPerson("pers1");
    Attachment attachment1 = createAttachment(organization1, negotiation1, creator);

    AttachmentViewDTO attachmentView =
        attachmentRepository.findAllById(attachment1.getId()).orElse(null);

    assertNotNull(attachmentView);
    assertEquals(attachmentView.getId(), attachment1.getId());
    assertEquals(attachmentView.getSize(), attachment1.getSize());
    assertEquals(attachmentView.getContentType(), attachment1.getContentType());
    assertEquals(attachmentView.getName(), attachment1.getName());
    assertEquals(attachmentView.getNegotiationId(), this.negotiation1.getId());
    assertEquals(attachmentView.getOrganizationExternalId(), organization1.getExternalId());
    assertEquals(attachmentView.getCreatedById(), attachment1.getCreatedBy().getId());
    assertEquals(attachmentView.getPayload().length, 2);
    assertEquals(attachmentView.getPayload()[0], attachment1.getPayload()[0]);
    assertEquals(attachmentView.getPayload()[1], attachment1.getPayload()[1]);
  }

  @Test
  void getPayloadById_notFound() {
    Person creator = createPerson("pers1");
    createAttachment(organization1, negotiation1, creator);
    assertNull(attachmentRepository.findById("unknown").orElse(null));
  }

  @Test
  void save_1_ok() {
    Person creator = createPerson("pers1");
    createAttachment(organization1, negotiation1, creator);
    assertEquals(1, attachmentRepository.findAll().size());
  }

  @Test
  void save_1000_ok() {
    Person creator = createPerson("pers1");
    for (int i = 0; i < 1000; i++) {
      createAttachment(organization1, negotiation1, creator);
    }
    assertEquals(1000, attachmentRepository.findAll().size());
  }
}
