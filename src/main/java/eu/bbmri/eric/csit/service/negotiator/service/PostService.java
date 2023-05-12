package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.post.PostCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.post.PostDTO;
import java.util.List;

public interface PostService {

  PostDTO create(PostCreateDTO postRequest, Long personId, String negotiationId);

  List<PostDTO> findByNegotiationId(String negotiationId);

  PostDTO update(PostCreateDTO updateRequest, String negotiationId, String postId);

}
