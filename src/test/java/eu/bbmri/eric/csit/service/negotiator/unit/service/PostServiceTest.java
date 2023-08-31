package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostType;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PostRepository;
import eu.bbmri.eric.csit.service.negotiator.service.PostServiceImpl;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;

public class PostServiceTest {

  @Mock PostRepository postRepository;

  @Mock ModelMapper modelMapper;

  @InjectMocks PostServiceImpl postService;

  private AutoCloseable closeable;

  private Post publicPost1;

  private Post publicPost2;

  private Post privatePost1;

  private Post privatePost2;

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
    Negotiation n = new Negotiation();
    Resource r1 = new Resource();
    r1.setSourceId("r1");
    Resource r2 = new Resource();
    r2.setSourceId("r1");
    publicPost1 = new Post();
    publicPost1.setType(PostType.PUBLIC);
    publicPost1.setNegotiation(n);
    publicPost1.setText("public post 1");
    publicPost2 = new Post();
    publicPost2.setType(PostType.PUBLIC);
    publicPost2.setNegotiation(n);
    publicPost2.setText("public post 2");
    privatePost1 = new Post();
    privatePost1.setType(PostType.PRIVATE);
    privatePost1.setResource(r1);
    privatePost1.setText("private post 1");
    privatePost2 = new Post();
    privatePost2.setType(PostType.PRIVATE);
    privatePost2.setResource(r2);
    privatePost2.setText("private post 2");
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  @Test
  public void test_FindPostByNegotiationId() {
    when(postRepository.findByNegotiationId("negotiationId"))
        .thenReturn(List.of(publicPost1, publicPost2));
    Assertions.assertEquals(
        2,
        postService
            .findByNegotiationId("negotiationId", Optional.empty(), Optional.empty())
            .size());
  }

  @Test
  public void test_FindPostByNegotiationIdNoResults() {
    when(postRepository.findByNegotiationId("fakeId")).thenReturn(Collections.emptyList());
    Assertions.assertEquals(
        0, postService.findByNegotiationId("fakeId", Optional.empty(), Optional.empty()).size());
  }

  @Test
  public void test_FindPostByNegotiationIdAndPosters() {
    when(postRepository.findNewByNegotiationIdAndPosters("negotiationId", List.of("p1")))
        .thenReturn(List.of(publicPost1, publicPost2));
    Assertions.assertEquals(
        2,
        postService
            .findNewByNegotiationIdAndPosters(
                "negotiationId", List.of("p1"), Optional.empty(), Optional.empty())
            .size());
  }

  @Test
  public void test_FindPostByNegotiationIdAndPosters_ReturnsEmptyList_whenNotFound() {
    when(postRepository.findNewByNegotiationIdAndPosters(any(), any()))
        .thenReturn(Collections.emptyList());
    assertTrue(
        postService
            .findNewByNegotiationIdAndPosters(
                "fakeID", Arrays.asList("fakep1", "fakeP2"), Optional.empty(), Optional.empty())
            .isEmpty());
  }

  @Test
  public void test_FindAllPrivatePosts() {
    when(postRepository.findByNegotiationIdAndType("negotiationId", Optional.of(PostType.PRIVATE)))
        .thenReturn(List.of(privatePost1, privatePost2));
    Assertions.assertEquals(
        2,
        postService
            .findByNegotiationId("negotiationId", Optional.of(PostType.PRIVATE), Optional.empty())
            .size());
  }

  @Test
  public void test_FindAllPrivatePostsByResource() {
    when(postRepository.findByNegotiationIdAndTypeAndResource(
            "negotiationId", Optional.of(PostType.PRIVATE), Optional.of("r1")))
        .thenReturn(List.of(privatePost1));
    Assertions.assertEquals(
        1,
        postService
            .findByNegotiationId("negotiationId", Optional.of(PostType.PRIVATE), Optional.of("r1"))
            .size());
  }
}
