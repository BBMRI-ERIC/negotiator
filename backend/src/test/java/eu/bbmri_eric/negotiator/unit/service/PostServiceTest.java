package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationAccessManager;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostCreateDTO;
import eu.bbmri_eric.negotiator.post.PostDTO;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.post.PostServiceImpl;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class PostServiceTest {

  private static final long RESEARCHER_ID = 2L;
  private static final String RESEARCHER_AUTH_NAME = "researcher";
  private static final String RESEARCHER_AUTH_SUBJECT = "researcher@aai.eu";
  private static final String RESEARCHER_AUTH_EMAIL = "researcher@aai.eu";
  private static final long BIOBANKER_1_ID = 3L;
  private static final String BIOBANKER_1_AUTH_NAME = "biobanker_1";
  private static final String BIOBANKER_1_AUTH_SUBJECT = "biobanker_1@aai.eu";
  private static final String BIOBANKER_1_AUTH_EMAIL = "biobanker_1@aai.eu";
  private static final long BIOBANKER_2_ID = 4L;
  private static final String BIOBANKER_2_AUTH_NAME = "biobanker_2";
  private static final String BIOBANKER_2_AUTH_SUBJECT = "biobanker_2@aai.eu";
  private static final String BIOBANKER_2_AUTH_EMAIL = "biobanker_2@aai.eu";
  private static final long BIOBANKER_3_ID = 5L;
  private static final String BIOBANKER_3_AUTH_NAME = "biobanker_3";
  private static final String BIOBANKER_3_AUTH_SUBJECT = "biobanker_3@aai.eu";
  private static final String BIOBANKER_3_AUTH_EMAIL = "biobanker_3@aai.eu";
  private static final String ADMIN_AUTH_NAME = "admin";
  private static final String ADMIN_AUTH_SUBJECT = "admin@aai.eu";
  private static final String ADMIN_AUTH_EMAIL = "admin@aai.eu";
  private static final String ORG_1 = "Organization_1";
  private static final String ORG_2 = "Organization_2";
  private static final String NEG_1 = "negotiationId";
  @Mock PostRepository postRepository;
  @Mock NegotiationRepository negotiationRepository;
  @Mock OrganizationRepository organizationRepository;
  @Mock PersonRepository personRepository;
  @Mock ApplicationEventPublisher applicationEventPublisher;
  @Mock PersonService personService;
  @Mock NegotiationService negotiationService;
  @Mock NegotiationAccessManager negotiationAccessManager;

  @Mock ModelMapper modelMapper;
  @InjectMocks PostServiceImpl postService;
  private AutoCloseable closeable;

  private Post publicPost1;
  private Post publicPost2;
  private Post privateResToOrg1;
  private Post privateResToOrg2;
  private Post privateBio1ToOrg1;
  private Post privateBio2ToOrg2;
  private List<Post> allPosts;
  private List<Post> publicPosts;
  private List<Post> privatePosts;
  private Negotiation negotiation;
  private Organization organization1;
  private Organization organization2;
  private Person researcher;
  private Person biobanker1;
  private Person biobanker2;

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);

    researcher =
        Person.builder()
            .id(RESEARCHER_ID)
            .name(RESEARCHER_AUTH_NAME)
            .email(RESEARCHER_AUTH_EMAIL)
            .subjectId(RESEARCHER_AUTH_SUBJECT)
            .build();

    biobanker1 =
        Person.builder()
            .id(BIOBANKER_1_ID)
            .name(BIOBANKER_1_AUTH_NAME)
            .email(BIOBANKER_1_AUTH_EMAIL)
            .subjectId(BIOBANKER_1_AUTH_SUBJECT)
            .build();

    biobanker2 =
        Person.builder()
            .id(BIOBANKER_2_ID)
            .name(BIOBANKER_2_AUTH_NAME)
            .email(BIOBANKER_2_AUTH_EMAIL)
            .subjectId(BIOBANKER_2_AUTH_SUBJECT)
            .build();

    DiscoveryService discoveryService = new DiscoveryService();

    organization1 = Organization.builder().id(1L).externalId(ORG_1).build();
    organization2 = Organization.builder().id(2L).externalId(ORG_2).build();

    Resource resource1 =
        Resource.builder()
            .discoveryService(discoveryService)
            .sourceId("resource:1")
            .name("Resource 1")
            .organization(organization1)
            .build();

    Resource resource2 =
        Resource.builder()
            .discoveryService(discoveryService)
            .sourceId("resource:2")
            .name("Resource 2")
            .organization(organization2)
            .build();
    organization1.setResources(Set.of(resource1));
    organization2.setResources(Set.of(resource2));

    negotiation =
        Negotiation.builder()
            .resources(Set.of(resource1, resource2))
            .id(NEG_1)
            .humanReadable("#1 Material Type: DNA")
            .build();
    negotiation.setCreatedBy(researcher);

    publicPost1 =
        TestUtils.createPost(negotiation, researcher, null, "public post 1", PostType.PUBLIC);
    publicPost2 =
        TestUtils.createPost(negotiation, researcher, null, "public post 2", PostType.PUBLIC);
    privateResToOrg1 =
        TestUtils.createPost(
            negotiation,
            researcher,
            organization1,
            "private post from resercher to organization 1",
            PostType.PRIVATE);
    privateResToOrg2 =
        TestUtils.createPost(
            negotiation,
            researcher,
            organization2,
            "private post from resercher to organization 2",
            PostType.PRIVATE);
    privateBio1ToOrg1 =
        TestUtils.createPost(
            negotiation,
            biobanker1,
            organization1,
            "private post from biobanker 1 to organization 1",
            PostType.PRIVATE);
    privateBio2ToOrg2 =
        TestUtils.createPost(
            negotiation,
            biobanker2,
            organization2,
            "private post from biobanker 2 to organization 2",
            PostType.PRIVATE);

    publicPosts = List.of(publicPost1, publicPost2);
    privatePosts =
        List.of(privateResToOrg1, privateResToOrg2, privateBio1ToOrg1, privateBio2ToOrg2);
    allPosts =
        List.of(
            publicPost1,
            publicPost2,
            privateResToOrg1,
            privateResToOrg2,
            privateBio2ToOrg2,
            privateBio1ToOrg1);

    when(personService.isRepresentativeOfAnyResourceOfOrganization(
            BIOBANKER_1_ID, organization1.getId()))
        .thenReturn(true);
    when(negotiationRepository.existsById(NEG_1)).thenReturn(true);
    // For void methods, use doThrow instead of thenThrow

    when(personService.isRepresentativeOfAnyResourceOfOrganization(
            BIOBANKER_1_ID, organization2.getId()))
        .thenReturn(false);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  @Test
  public void test_createPublicForNegotiationId_isForbidden_whenPublicPostsAreDisabled() {
    when(negotiationService.isAuthorizedForNegotiation(negotiation.getId())).thenReturn(true);
    negotiation.setPublicPostsEnabled(false);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));
    PostCreateDTO postCreateDTO =
        PostCreateDTO.builder().text("message").type(PostType.PUBLIC).build();
    ForbiddenRequestException exception =
        assertThrows(
            ForbiddenRequestException.class,
            () -> postService.create(postCreateDTO, negotiation.getId()));
    assertEquals(
        exception.getMessage(), "PUBLIC posts are not currently allowed for this negotiation");
  }

  @Test
  public void test_createPrivateForNegotiationId_isForbidden_whenPrivatePostsAreDisabled() {
    when(negotiationService.isAuthorizedForNegotiation(negotiation.getId())).thenReturn(true);
    negotiation.setPrivatePostsEnabled(false);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));
    PostCreateDTO postCreateDTO =
        PostCreateDTO.builder()
            .text("message")
            .organizationId(ORG_1)
            .type(PostType.PRIVATE)
            .build();
    ForbiddenRequestException exception =
        assertThrows(
            ForbiddenRequestException.class,
            () -> postService.create(postCreateDTO, negotiation.getId()));
    assertEquals(
        exception.getMessage(), "PRIVATE posts are not currently allowed for this negotiation");
  }

  @Test
  @WithMockNegotiatorUser(id = 4L)
  public void
      test_createPrivateForNegotiationId_isForbidden_whenUserIsNotAuthorizedForNegotiation() {
    when(negotiationService.isAuthorizedForNegotiation(negotiation.getId())).thenReturn(false);
    negotiation.setPrivatePostsEnabled(true);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));

    PostCreateDTO postCreateDTO =
        PostCreateDTO.builder()
            .text("message")
            .organizationId(ORG_1)
            .type(PostType.PRIVATE)
            .build();

    ForbiddenRequestException exception =
        assertThrows(
            ForbiddenRequestException.class,
            () -> postService.create(postCreateDTO, negotiation.getId()));
    assertEquals(
        exception.getMessage(), "You're not authorized to send messages to this negotiation");
  }

  @Test
  @WithMockNegotiatorUser(id = 4L)
  public void
      test_createPublicForNegotiationId_isForbidden_whenUserIsNotAuthorizedForNegotiation() {
    negotiation.setPublicPostsEnabled(true);
    negotiation.setPrivatePostsEnabled(true);
    when(negotiationService.isAuthorizedForNegotiation(negotiation.getId())).thenReturn(false);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));

    PostCreateDTO postCreateDTO =
        PostCreateDTO.builder().text("message").type(PostType.PUBLIC).build();
    ForbiddenRequestException exception =
        assertThrows(
            ForbiddenRequestException.class,
            () -> postService.create(postCreateDTO, negotiation.getId()));
    assertEquals(
        exception.getMessage(), "You're not authorized to send messages to this negotiation");
  }

  @Test
  @WithMockNegotiatorUser(id = RESEARCHER_ID)
  public void test_createPrivateForNegotiationId_Ok() {
    negotiation.setPrivatePostsEnabled(true);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));
    when(organizationRepository.findByExternalId(any())).thenReturn(Optional.of(organization1));
    when(personRepository.findById(any())).thenReturn(Optional.of(researcher));
    when(postRepository.save(any())).thenReturn(privateResToOrg1);
    when(negotiationService.isAuthorizedForNegotiation(negotiation.getId())).thenReturn(true);

    PostCreateDTO postCreateDTO =
        PostCreateDTO.builder()
            .text(privateResToOrg1.getText())
            .organizationId(ORG_1)
            .type(PostType.PRIVATE)
            .build();
    PostDTO postDTO =
        PostDTO.builder()
            .id("test-id")
            .creationDate(LocalDateTime.now())
            .createdBy(new UserResponseModel())
            .text(privateResToOrg1.getText())
            .type(PostType.PRIVATE)
            .build();
    when(modelMapper.map(privateResToOrg1, PostDTO.class)).thenReturn(postDTO);
    PostDTO returnedPostDTO = postService.create(postCreateDTO, negotiation.getId());
    assertEquals(returnedPostDTO.getText(), privateResToOrg1.getText());
    assertEquals(returnedPostDTO.getType(), PostType.PRIVATE);
  }

  @Test
  @WithMockNegotiatorUser(id = RESEARCHER_ID)
  public void test_createPosts_failWithEntityNotStorableException_whenDataIntegrityViolation() {
    negotiation.setPrivatePostsEnabled(true);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));
    when(organizationRepository.findByExternalId(any())).thenReturn(Optional.of(organization1));
    when(personRepository.findById(any())).thenReturn(Optional.of(researcher));
    when(postRepository.save(any())).thenThrow(new DataIntegrityViolationException(""));
    when(negotiationService.isAuthorizedForNegotiation(negotiation.getId())).thenReturn(true);

    PostCreateDTO postCreateDTO =
        PostCreateDTO.builder()
            .text(privateResToOrg1.getText())
            .organizationId(ORG_1)
            .type(PostType.PRIVATE)
            .build();
    assertThrows(
        EntityNotStorableException.class,
        () -> {
          postService.create(postCreateDTO, negotiation.getId());
        });
  }

  @Test
  @WithMockNegotiatorUser(id = RESEARCHER_ID)
  public void test_createPublicForNegotiationId_Ok() {
    negotiation.setPublicPostsEnabled(true);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));
    when(organizationRepository.findByExternalId(any())).thenReturn(Optional.of(organization1));
    when(personRepository.findById(any())).thenReturn(Optional.of(researcher));
    when(postRepository.save(any())).thenReturn(publicPost1);
    when(negotiationService.isAuthorizedForNegotiation(negotiation.getId())).thenReturn(true);

    PostCreateDTO postCreateDTO =
        PostCreateDTO.builder().text(publicPost1.getText()).type(PostType.PUBLIC).build();
    PostDTO postDTO =
        PostDTO.builder()
            .id("test-id")
            .createdBy(new UserResponseModel())
            .creationDate(LocalDateTime.now())
            .text(publicPost1.getText())
            .type(PostType.PUBLIC)
            .build();
    when(modelMapper.map(publicPost1, PostDTO.class)).thenReturn(postDTO);
    PostDTO returnedPostDTO = postService.create(postCreateDTO, negotiation.getId());
    assertEquals(returnedPostDTO.getText(), publicPost1.getText());
    assertEquals(returnedPostDTO.getType(), PostType.PUBLIC);
  }

  @Test
  public void test_findByNegotiationId_NoResults() {
    when(postRepository.findByNegotiationId("fakeId")).thenReturn(Collections.emptyList());
    assertThrows(
        EntityNotFoundException.class, () -> postService.findByNegotiationId("fakeId").size());
  }

  /** Tests that the admin gets all the posts of a negotiation */
  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findByNegotiationId_AsAdmin_All() {
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(allPosts);
    assertEquals(allPosts.size(), postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that the researcger gets all the posts of a negotiation */
  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findByNegotiationId_AsResearcher_All() {
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(allPosts);
    when(negotiationService.isNegotiationCreator(NEG_1)).thenReturn(true);
    assertEquals(allPosts.size(), postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that the biobanker gets the public posts and the ones sent to their organization */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  @Disabled
  public void test_findByNegotiationId_AsBiobanker_All() {
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(allPosts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    assertEquals(4, postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that a person not involved in the negotiation, gets no posts */
  @Disabled
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void test_findByNegotiationId_AsUserUnauthorizedForNegotiation_All() {
    when(postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
            NEG_1, PostType.PRIVATE, "organization:1"))
        .thenReturn(allPosts);
    doThrow(ForbiddenRequestException.class)
        .when(negotiationAccessManager)
        .verifyReadAccessForNegotiation(any(), any());
    assertThrows(ForbiddenRequestException.class, () -> postService.findByNegotiationId(NEG_1));
  }

  /** Tests that the admin gets all the public posts of a negotiation */
  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findByNegotiationId_AsAdmin_Public() {
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(publicPosts);
    assertEquals(publicPosts.size(), postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that the researcher gets all the public posts */
  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findByNegotiationId_AsResearcher_Public() {
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(publicPosts);
    when(negotiationService.isNegotiationCreator(NEG_1)).thenReturn(true);
    assertEquals(publicPosts.size(), postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that the biobanker gets the public posts */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  @Disabled
  public void test_findByNegotiationId_AsBiobanker_Public() {
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(publicPosts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    assertEquals(publicPosts.size(), postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that a person not involved in the negotiation, gets no posts */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void test_findByNegotiationId_AsUserUnauthorizedForNegotiation_Public() {
    when(postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
            NEG_1, PostType.PRIVATE, "organization:1"))
        .thenReturn(publicPosts);
    doThrow(ForbiddenRequestException.class)
        .when(negotiationAccessManager)
        .verifyReadAccessForNegotiation(any(), any());
    assertThrows(ForbiddenRequestException.class, () -> postService.findByNegotiationId(NEG_1));
  }

  /** Tests that the admin gets all the private posts of a negotiation */
  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findByNegotiationId_AsAdmin_Private() {
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(privatePosts);
    assertEquals(privatePosts.size(), postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that the researcher gets all the private posts */
  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findByNegotiationId_AsResearcher_Private() {
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(privatePosts);
    when(negotiationService.isNegotiationCreator(NEG_1)).thenReturn(true);
    assertEquals(privatePosts.size(), postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that the biobanker gets the private posts sent to their organization */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  @Disabled
  public void test_findByNegotiationId_AsBiobanker_Private() {
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(privatePosts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    assertEquals(2, postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that a person not involved in the negotiation, gets no posts */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void test_findByNegotiationId_AsUserUnauthorizedForNegotiation_Private() {
    when(postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
            NEG_1, PostType.PRIVATE, "organization:1"))
        .thenReturn(privatePosts);
    when(personRepository.findById(any())).thenReturn(Optional.of(biobanker2));
    assertEquals(0, postService.findByNegotiationId(NEG_1).size());
  }

  /**
   * Tests that the admin gets all the private posts of a negotiation sent to a specific
   * organization
   */
  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findByNegotiationId_AsAdmin_Private_withOrganizationId() {
    List<Post> posts = List.of(privateResToOrg1, privateBio1ToOrg1);
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(posts);
    assertEquals(posts.size(), postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that the researcher gets all the private posts */
  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findByNegotiationId_AsResearcher_Private_withOrganizationId() {
    List<Post> posts = List.of(privateResToOrg1, privateBio1ToOrg1);
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(posts);
    when(negotiationService.isNegotiationCreator(NEG_1)).thenReturn(true);
    when(personRepository.findById(any())).thenReturn(Optional.of(researcher));
    assertEquals(posts.size(), postService.findByNegotiationId(NEG_1).size());
  }

  /** Tests that the biobanker gets the public posts */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  @Disabled
  public void test_findByNegotiationId_AsBiobanker_Private_withOrganizationId() {
    List<Post> posts = List.of(privateResToOrg1, privateBio1ToOrg1);
    when(postRepository.findByNegotiationId(NEG_1)).thenReturn(posts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    when(personRepository.findById(any())).thenReturn(Optional.of(biobanker1));
    assertEquals(2, postService.findByNegotiationId(NEG_1).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void
      test_findByNegotiationId_AsUserUnauthorizedForNegotiation_Private_withOrganizationId() {
    List<Post> posts = List.of(privateResToOrg1, privateBio1ToOrg1);
    when(postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
            NEG_1, PostType.PRIVATE, "organization:1"))
        .thenReturn(posts);
    when(personRepository.findById(any())).thenReturn(Optional.of(biobanker2));
    assertEquals(0, postService.findByNegotiationId(NEG_1).size());
  }
}
