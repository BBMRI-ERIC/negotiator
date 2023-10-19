package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetailsService;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostType;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.post.PostCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.post.PostDTO;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import eu.bbmri.eric.csit.service.negotiator.service.PostService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin
public class PostController {

  @Autowired private PostService postService;

  @Autowired private NegotiationService negotiationService;

  @PostMapping(
      value = "/negotiations/{negotiationId}/posts",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  PostDTO add(
      @Valid @RequestBody PostCreateDTO request, @Valid @PathVariable String negotiationId) {
    return postService.create(
        request,
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
        negotiationId);
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
    NegotiationDTO n = negotiationService.findById(negotiationId, true);

    List<PersonRoleDTO> negotiationPersonsWithRoles =
        n.getPersons().stream().filter(p -> p.getRole().equals(roleName)).toList();

    List<String> posters =
        negotiationPersonsWithRoles.stream().map(PersonRoleDTO::getName).toList();

    return postService.findNewByNegotiationIdAndAuthors(negotiationId, posters, type, resource);
  }

  @PutMapping("/negotiations/{negotiationId}/posts/{postId}")
  PostDTO update(
      @Valid @RequestBody PostCreateDTO request,
      @Valid @PathVariable String negotiationId,
      @Valid @PathVariable String postId) {
    return postService.update(request, negotiationId, postId);
  }
}
