package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.post.PostCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.post.PostDTO;

public interface PostService {

  PostDTO create(PostCreateDTO postRequest, Long personId);

}
