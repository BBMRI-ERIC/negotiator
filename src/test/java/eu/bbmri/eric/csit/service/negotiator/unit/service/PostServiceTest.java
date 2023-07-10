package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PostRepository;
import eu.bbmri.eric.csit.service.negotiator.service.PostServiceImpl;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  @Test
  public void test_FindByNegotiationId() {
    when(postRepository.findByNegotiationId("negotiationId"))
        .thenReturn(List.of(new Post(), new Post()));
    Assertions.assertEquals(2, postService.findByNegotiationId("negotiationId").size());
  }

  @Test
  public void test_FindByNegotiationIdNoResults() {
    when(postRepository.findByNegotiationId("fakeId")).thenReturn(Collections.emptyList());
    Assertions.assertEquals(0, postService.findByNegotiationId("fakeId").size());
  }

  @Test
  public void test_FindByNegotiationIdAndPosters() {
    when(postRepository.findNewByNegotiationIdAndPosters("negotiationId", List.of("p1")))
        .thenReturn(List.of(new Post(), new Post()));
    Assertions.assertEquals(
        2, postService.findNewByNegotiationIdAndPosters("negotiationId", List.of("p1")).size());
  }

  @Test
  public void test_FindByNegotiationIdAndPosters_ReturnsEmptyList_whenNotFound() {
    when(postRepository.findNewByNegotiationIdAndPosters(any(), any()))
        .thenReturn(Collections.emptyList());
    assertTrue(
        postService
            .findNewByNegotiationIdAndPosters("fakeID", Arrays.asList("fakep1", "fakeP2"))
            .isEmpty());
  }
}
