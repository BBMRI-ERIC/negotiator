package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.post.PostCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.post.PostDTO;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.service.PostServiceImpl;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@CrossOrigin
public class PostController {

  @Autowired
  private PostServiceImpl postService;
  @Autowired
  private ModelMapper modelMapper;

  @PostMapping(
      value = "/posts",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  PostDTO add(@Valid @RequestBody PostCreateDTO request) {
    PostDTO post = postService.create(request, getPersonId());
    return post;

  }

  private Long getPersonId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return ((NegotiatorUserDetails) auth.getPrincipal()).getPerson().getId();
  }

}
