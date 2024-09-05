package eu.bbmri_eric.negotiator.post;

import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@Tag(name = "Comments", description = "Submit and retrieve comments")
@SecurityRequirement(name = "security_auth")
public class PostController {

  private final PostService postService;

  private final NegotiationService negotiationService;

  private final PostModelAssembler postModelAssembler;

  public PostController(
      PostService postService,
      NegotiationService negotiationService,
      PostModelAssembler postModelAssembler) {
    this.postService = postService;
    this.negotiationService = negotiationService;
    this.postModelAssembler = postModelAssembler;
  }

  @PostMapping(
      value = "/negotiations/{negotiationId}/posts",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaTypes.HAL_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  EntityModel<PostDTO> add(
      @Valid @RequestBody PostCreateDTO request, @Valid @PathVariable String negotiationId) {
    return postModelAssembler.toModel(postService.create(request, negotiationId));
  }

  @GetMapping("/negotiations/{negotiationId}/posts")
  List<PostDTO> getAllMessagesByNegotiation(
      @Valid @PathVariable String negotiationId,
      @RequestParam(value = "role", required = false) String roleName,
      @RequestParam(value = "type", required = false) PostType type,
      @RequestParam(value = "resource", required = false) String resource) {
    if (roleName == null || roleName.isEmpty()) {
      return postService.findByNegotiationId(negotiationId, type, resource);
    }
    NegotiationDTO negotiationDTO = negotiationService.findById(negotiationId, true);

    List<String> posters = List.of(negotiationDTO.getAuthor().getName());

    return postService.findNewByNegotiationIdAndAuthors(negotiationId, posters, type, resource);
  }

  @PutMapping("/negotiations/{negotiationId}/posts/{postId}")
  PostDTO update(
      @Valid @RequestBody PostCreateDTO createDTO,
      @Valid @PathVariable String negotiationId,
      @Valid @PathVariable String postId) {
    return postService.update(createDTO, negotiationId, postId);
  }

  @GetMapping(value = "/posts/{postId}", produces = MediaTypes.HAL_JSON_VALUE)
  @Operation(summary = "Find a post by an id")
  EntityModel<PostDTO> getById(@PathVariable @Valid String postId) {
    return postModelAssembler.toModel(postService.findById(postId));
  }
}
