package eu.bbmri_eric.negotiator.post;

import java.util.List;

/** Interface for managing Posts. */
public interface PostService {

  /**
   * Creates a new post for the specified Negotiation
   *
   * @param postRequest the Post DTO containing request information
   * @param negotiationId the ID of the negotiation to which the post refers
   * @return the response PostDTO object
   */
  PostDTO create(PostCreateDTO postRequest, String negotiationId);

  /**
   * Find a post by id.
   *
   * @param id the id of the post
   * @return post
   */
  PostDTO findById(String id);

  /**
   * Finds all the posts related to a Negotiation
   *
   * @param negotiationId the ID of the negotiation
   * @return a list of Posts the user is allowed to see
   */
  List<PostDTO> findByNegotiationId(String negotiationId);

  /**
   * Updates a specific post
   *
   * @param updateRequest the Post DTO containing information to be updated
   * @param negotiationId the ID of the negotiation
   * @param postId the ID of the post to update
   * @return the response PostDTO object
   */
  PostDTO update(PostCreateDTO updateRequest, String negotiationId, String postId);
}
