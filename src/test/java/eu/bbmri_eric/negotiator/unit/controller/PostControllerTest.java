package eu.bbmri_eric.negotiator.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.post.PostController;
import eu.bbmri_eric.negotiator.post.PostDTO;
import eu.bbmri_eric.negotiator.post.PostModelAssembler;
import eu.bbmri_eric.negotiator.post.PostService;
import eu.bbmri_eric.negotiator.post.PostStatus;
import eu.bbmri_eric.negotiator.post.PostType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(PostController.class)
@Import({PostModelAssembler.class})
public class PostControllerTest {
  @Autowired private MockMvc mvc;
  @Autowired private PostModelAssembler postModelAssembler;
  @MockBean private PostService postService;
  @MockBean private NegotiationService negotiationService;

  @Test
  @WithMockUser
  void getPostById_ok() throws Exception {
    PostDTO postDTO =
        PostDTO.builder()
            .id("test-id")
            .type(PostType.PUBLIC)
            .text("test comment")
            .creationDate(LocalDateTime.now())
            .status(PostStatus.CREATED)
            .build();
    when(postService.findById(any())).thenReturn(postDTO);
    mvc.perform(MockMvcRequestBuilders.get("/v3/posts/test-id"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(jsonPath("$.id").value("test-id"))
        .andExpect(jsonPath("$._links.self").isNotEmpty());
  }
}
