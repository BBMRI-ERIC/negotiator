package eu.bbmri_eric.negotiator.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.database.model.DataSource;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Post;
import eu.bbmri_eric.negotiator.database.model.PostStatus;
import eu.bbmri_eric.negotiator.database.model.PostType;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.PostRepository;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.service.NegotiationService;
import eu.bbmri_eric.negotiator.service.PersonService;
import eu.bbmri_eric.negotiator.service.PostServiceImpl;
import eu.bbmri_eric.negotiator.unit.context.WithMockNegotiatorUser;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
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
  @Mock PostRepository postRepository;

  @Mock PersonService personService;

  @Mock NegotiationService negotiationService;
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

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);

    Person researcher =
        Person.builder()
            .id(RESEARCHER_ID)
            .name(RESEARCHER_AUTH_NAME)
            .email(RESEARCHER_AUTH_EMAIL)
            .subjectId(RESEARCHER_AUTH_SUBJECT)
            .build();

    Person biobanker1 =
        Person.builder()
            .id(BIOBANKER_1_ID)
            .name(BIOBANKER_1_AUTH_NAME)
            .email(BIOBANKER_1_AUTH_EMAIL)
            .subjectId(BIOBANKER_1_AUTH_SUBJECT)
            .build();

    Person biobanker2 =
        Person.builder()
            .id(BIOBANKER_2_ID)
            .name(BIOBANKER_2_AUTH_NAME)
            .email(BIOBANKER_2_AUTH_EMAIL)
            .subjectId(BIOBANKER_2_AUTH_SUBJECT)
            .build();

    DataSource dataSource = new DataSource();

    Organization organization1 = Organization.builder().externalId(ORG_1).build();
    Organization organization2 = Organization.builder().externalId(ORG_2).build();

    Resource resource1 =
        Resource.builder()
            .dataSource(dataSource)
            .sourceId("resource:1")
            .name("Resource 1")
            .organization(organization1)
            .build();

    Resource resource2 =
        Resource.builder()
            .dataSource(dataSource)
            .sourceId("resource:2")
            .name("Resource 2")
            .organization(organization2)
            .build();
    organization1.setResources(Set.of(resource1));
    organization2.setResources(Set.of(resource2));

    Request request = Request.builder().resources(Set.of(resource1, resource2)).build();

    Negotiation negotiation = Negotiation.builder().requests(Set.of(request)).build();
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
            "private post from resercher to organization 1",
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
    when(personService.isRepresentativeOfAnyResource(BIOBANKER_1_ID, List.of("resource:1")))
        .thenReturn(true);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  @Test
  public void test_findByNegotiationId_NoResults() {
    when(postRepository.findByNegotiationId("fakeId")).thenReturn(Collections.emptyList());
    Assertions.assertEquals(0, postService.findByNegotiationId("fakeId", null, null).size());
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
    when(postRepository.findByNegotiationId("negotiationId")).thenReturn(allPosts);
    Assertions.assertEquals(
        allPosts.size(), postService.findByNegotiationId("negotiationId", null, null).size());
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
    when(postRepository.findByNegotiationId("negotiationId")).thenReturn(allPosts);
    Assertions.assertEquals(
        allPosts.size(), postService.findByNegotiationId("negotiationId", null, null).size());
  }

  /** Tests that the biobanker gets the public posts and the ones sent to their organization */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findByNegotiationId_AsBiobanker_All() {
    when(postRepository.findByNegotiationId("negotiationId")).thenReturn(allPosts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(4, postService.findByNegotiationId("negotiationId", null, null).size());
  }

  /** Tests that a person not involved in the negotiation, gets no posts */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void test_findByNegotiationId_AsUserUnauthorizedForNegotiation_All() {
    when(postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
            "negotiationId", PostType.PRIVATE, "organization:1"))
        .thenReturn(allPosts);
    Assertions.assertEquals(
        0,
        postService
            .findByNegotiationId("negotiationId", PostType.PRIVATE, "organization:1")
            .size());
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
    when(postRepository.findByNegotiationIdAndType("negotiationId", PostType.PUBLIC))
        .thenReturn(publicPosts);
    Assertions.assertEquals(
        publicPosts.size(),
        postService.findByNegotiationId("negotiationId", PostType.PUBLIC, null).size());
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
    when(postRepository.findByNegotiationIdAndType("negotiationId", PostType.PUBLIC))
        .thenReturn(publicPosts);
    Assertions.assertEquals(
        publicPosts.size(),
        postService.findByNegotiationId("negotiationId", PostType.PUBLIC, null).size());
  }

  /** Tests that the biobanker gets the public posts */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findByNegotiationId_AsBiobanker_Public() {
    when(postRepository.findByNegotiationIdAndType("negotiationId", PostType.PUBLIC))
        .thenReturn(publicPosts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(
        publicPosts.size(),
        postService.findByNegotiationId("negotiationId", PostType.PUBLIC, null).size());
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
            "negotiationId", PostType.PRIVATE, "organization:1"))
        .thenReturn(publicPosts);
    Assertions.assertEquals(
        0,
        postService
            .findByNegotiationId("negotiationId", PostType.PRIVATE, "organization:1")
            .size());
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
    when(postRepository.findByNegotiationIdAndType("negotiationId", PostType.PRIVATE))
        .thenReturn(privatePosts);
    Assertions.assertEquals(
        privatePosts.size(),
        postService.findByNegotiationId("negotiationId", PostType.PRIVATE, null).size());
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
    when(postRepository.findByNegotiationIdAndType("negotiationId", PostType.PRIVATE))
        .thenReturn(privatePosts);
    Assertions.assertEquals(
        privatePosts.size(),
        postService.findByNegotiationId("negotiationId", PostType.PRIVATE, null).size());
  }

  /** Tests that the biobanker gets the private posts sent to their organization */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findByNegotiationId_AsBiobanker_Private() {
    when(postRepository.findByNegotiationIdAndType("negotiationId", PostType.PRIVATE))
        .thenReturn(privatePosts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(
        2, postService.findByNegotiationId("negotiationId", PostType.PRIVATE, null).size());
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
            "negotiationId", PostType.PRIVATE, "organization:1"))
        .thenReturn(privatePosts);
    Assertions.assertEquals(
        0,
        postService
            .findByNegotiationId("negotiationId", PostType.PRIVATE, "organization:1")
            .size());
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
    when(postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
            "negotiationId", PostType.PRIVATE, "organization:1"))
        .thenReturn(posts);
    Assertions.assertEquals(
        posts.size(),
        postService
            .findByNegotiationId("negotiationId", PostType.PRIVATE, "organization:1")
            .size());
  }

  /** Tests that the researcher gets all the public posts */
  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findByNegotiationId_AsResearcher_Private_withOrganizationId() {
    List<Post> posts = List.of(privateResToOrg1, privateBio1ToOrg1);
    when(postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
            "negotiationId", PostType.PRIVATE, "organization:1"))
        .thenReturn(posts);

    Assertions.assertEquals(
        posts.size(),
        postService
            .findByNegotiationId("negotiationId", PostType.PRIVATE, "organization:1")
            .size());
  }

  /** Tests that the biobanker gets the public posts */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findByNegotiationId_AsBiobanker_Private_withOrganizationId() {
    List<Post> posts = List.of(privateResToOrg1, privateBio1ToOrg1);
    when(postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
            "negotiationId", PostType.PRIVATE, "organization:1"))
        .thenReturn(posts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(
        2,
        postService
            .findByNegotiationId("negotiationId", PostType.PRIVATE, "organization:1")
            .size());
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
            "negotiationId", PostType.PRIVATE, "organization:1"))
        .thenReturn(posts);
    Assertions.assertEquals(
        0,
        postService
            .findByNegotiationId("negotiationId", PostType.PRIVATE, "organization:1")
            .size());
  }

  @Test
  public void test_findNewByNegotiationIdAndAuthors_NoResults() {
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);
    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, authors))
        .thenReturn(Collections.emptyList());
    Assertions.assertEquals(
        0, postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, null).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findNewByNegotiationIdAndAuthors_AsAdmin_All() {
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, authors))
        .thenReturn(allPosts);

    Assertions.assertEquals(
        allPosts.size(),
        postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, null).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findNewByNegotiationIdAndAuthors_AsResearcher_All() {
    List<Post> posts = List.of(publicPost1, publicPost2, privateResToOrg1, privateBio1ToOrg1);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, authors))
        .thenReturn(posts);

    Assertions.assertEquals(
        posts.size(),
        postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, null).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findNewByNegotiationIdAndAuthors_AsBiobanker_All() {
    List<Post> posts = List.of(publicPost1, publicPost2, privateResToOrg1, privateBio1ToOrg1);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);
    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, authors))
        .thenReturn(posts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(
        posts.size(),
        postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, null).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void test_findNewByNegotiationIdAndAuthors_AsUserUnauthorizedForNegotiation_All() {
    List<Post> posts = List.of(publicPost1, publicPost2, privateResToOrg1, privateBio1ToOrg1);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, authors))
        .thenReturn(posts);

    Assertions.assertEquals(
        0, postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, null).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findNewByNegotiationIdAndAuthors_AsAdmin_Public() {
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, PostType.PUBLIC, authors))
        .thenReturn(allPosts);

    Assertions.assertEquals(
        allPosts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PUBLIC, null)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findNewByNegotiationIdAndAuthors_AsResearcher_Public() {
    List<Post> posts = List.of(publicPost1, publicPost2);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, PostType.PUBLIC, authors))
        .thenReturn(posts);

    Assertions.assertEquals(
        posts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PUBLIC, null)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findNewByNegotiationIdAndAuthors_AsBiobanker_Public() {
    List<Post> posts = List.of(publicPost1, publicPost2);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, PostType.PUBLIC, authors))
        .thenReturn(posts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(
        posts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PUBLIC, null)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void test_findNewByNegotiationIdAndAuthors_AsUserUnauthorizedForNegotiation_Public() {
    List<Post> posts = List.of(publicPost1, publicPost2);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, authors))
        .thenReturn(posts);

    Assertions.assertEquals(
        0, postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, null).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findNewByNegotiationIdAndAuthors_AsAdmin_Private() {
    List<String> authors =
        List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT, BIOBANKER_2_AUTH_SUBJECT);
    when(postRepository.findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, PostType.PRIVATE, authors))
        .thenReturn(privatePosts);

    Assertions.assertEquals(
        privatePosts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PRIVATE, null)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findNewByNegotiationIdAndAuthors_AsResearcher_Private() {
    List<Post> posts =
        List.of(privateResToOrg1, privateResToOrg2, privateBio1ToOrg1, privateBio2ToOrg2);
    List<String> authors =
        List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT, BIOBANKER_2_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, PostType.PRIVATE, authors))
        .thenReturn(posts);

    Assertions.assertEquals(
        posts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PRIVATE, null)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findNewByNegotiationIdAndAuthors_AsBiobanker_Private() {
    List<Post> posts =
        List.of(privateResToOrg1, privateResToOrg2, privateBio1ToOrg1, privateBio2ToOrg2);
    List<String> authors =
        List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT, BIOBANKER_2_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, PostType.PRIVATE, authors))
        .thenReturn(posts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(
        2,
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PRIVATE, null)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void test_findNewByNegotiationIdAndAuthors_AsUserUnauthorizedForNegotiation_Private() {
    List<Post> posts =
        List.of(privateResToOrg1, privateResToOrg2, privateBio1ToOrg1, privateBio2ToOrg2);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameIn(
            "fakeId", PostStatus.CREATED, authors))
        .thenReturn(posts);

    Assertions.assertEquals(
        0, postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, null).size());
  }

  @Test
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_NoResults() {
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);
    when(postRepository
            .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                "fakeId", PostStatus.CREATED, null, authors, ORG_1))
        .thenReturn(Collections.emptyList());
    Assertions.assertEquals(
        0, postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, ORG_1).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_AsAdmin_All() {
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameInAndOrganization_ExternalId(
            "fakeId", PostStatus.CREATED, authors, ORG_1))
        .thenReturn(allPosts);

    Assertions.assertEquals(
        allPosts.size(),
        postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, ORG_1).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_AsResearcher_All() {
    List<Post> posts = List.of(publicPost1, publicPost2, privateResToOrg1, privateBio1ToOrg1);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameInAndOrganization_ExternalId(
            "fakeId", PostStatus.CREATED, authors, ORG_1))
        .thenReturn(posts);

    Assertions.assertEquals(
        posts.size(),
        postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, ORG_1).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_AsBiobanker_All() {
    List<Post> posts = List.of(publicPost1, publicPost2, privateResToOrg1, privateBio1ToOrg1);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameInAndOrganization_ExternalId(
            "fakeId", PostStatus.CREATED, authors, ORG_1))
        .thenReturn(posts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(
        posts.size(),
        postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, ORG_1).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void
      test_findNewByNegotiationIdAndAuthors_WithOrganizationId_AsUserUnauthorizedForOrganization_All() {
    List<Post> posts = List.of(publicPost1, publicPost2, privateResToOrg1, privateBio1ToOrg1);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameInAndOrganization_ExternalId(
            "fakeId", PostStatus.CREATED, authors, ORG_1))
        .thenReturn(posts);

    Assertions.assertEquals(
        0, postService.findNewByNegotiationIdAndAuthors("fakeId", authors, null, ORG_1).size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_Public_AsAdmin() {
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository
            .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                "fakeId", PostStatus.CREATED, PostType.PUBLIC, authors, ORG_1))
        .thenReturn(allPosts);

    Assertions.assertEquals(
        allPosts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PUBLIC, ORG_1)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_Public_AsResearcher() {
    List<Post> posts = List.of(publicPost1, publicPost2);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository
            .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                "fakeId", PostStatus.CREATED, PostType.PUBLIC, authors, ORG_1))
        .thenReturn(posts);

    Assertions.assertEquals(
        posts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PUBLIC, ORG_1)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_Public_AsBiobanker() {
    List<Post> posts = List.of(publicPost1, publicPost2);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository
            .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                "fakeId", PostStatus.CREATED, PostType.PUBLIC, authors, ORG_1))
        .thenReturn(posts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(
        posts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PUBLIC, ORG_1)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void
      test_findNewByNegotiationIdAndAuthors_WithOrganizationId_Public_AsUserUnauthorizedForOrganization() {
    List<Post> posts = List.of(publicPost1, publicPost2);
    List<String> authors = List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT);

    when(postRepository
            .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                "fakeId", PostStatus.CREATED, PostType.PUBLIC, authors, ORG_1))
        .thenReturn(posts);

    Assertions.assertEquals(
        0,
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PUBLIC, ORG_1)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_Private_AsAdmin() {
    List<String> authors =
        List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT, BIOBANKER_2_AUTH_SUBJECT);
    when(postRepository
            .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                "fakeId", PostStatus.CREATED, PostType.PRIVATE, authors, ORG_1))
        .thenReturn(privatePosts);

    Assertions.assertEquals(
        privatePosts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PRIVATE, ORG_1)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_Private_AsResearcher() {
    List<Post> posts =
        List.of(privateResToOrg1, privateResToOrg2, privateBio1ToOrg1, privateBio2ToOrg2);
    List<String> authors =
        List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT, BIOBANKER_2_AUTH_SUBJECT);

    when(postRepository
            .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                "fakeId", PostStatus.CREATED, PostType.PRIVATE, authors, ORG_1))
        .thenReturn(posts);

    Assertions.assertEquals(
        posts.size(),
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PRIVATE, ORG_1)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findNewByNegotiationIdAndAuthors_WithOrganizationId_Private_AsBiobanker() {
    List<Post> posts =
        List.of(privateResToOrg1, privateResToOrg2, privateBio1ToOrg1, privateBio2ToOrg2);
    List<String> authors =
        List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT, BIOBANKER_2_AUTH_SUBJECT);

    when(postRepository
            .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                "fakeId", PostStatus.CREATED, PostType.PRIVATE, authors, ORG_1))
        .thenReturn(posts);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    Assertions.assertEquals(
        2,
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PRIVATE, ORG_1)
            .size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_3_ID,
      authName = BIOBANKER_3_AUTH_NAME,
      authSubject = BIOBANKER_3_AUTH_SUBJECT,
      authEmail = BIOBANKER_3_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:3"})
  public void
      test_findNewByNegotiationIdAndAuthors_WithOrganizationId_Private_UserUnauthorizedForNegotiation() {
    List<Post> posts =
        List.of(privateResToOrg1, privateResToOrg2, privateBio1ToOrg1, privateBio2ToOrg2);
    List<String> authors =
        List.of(RESEARCHER_AUTH_SUBJECT, BIOBANKER_1_AUTH_SUBJECT, BIOBANKER_2_AUTH_SUBJECT);

    when(postRepository
            .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                "fakeId", PostStatus.CREATED, PostType.PRIVATE, authors, ORG_1))
        .thenReturn(posts);

    Assertions.assertEquals(
        0,
        postService
            .findNewByNegotiationIdAndAuthors("fakeId", authors, PostType.PRIVATE, ORG_1)
            .size());
  }
}
