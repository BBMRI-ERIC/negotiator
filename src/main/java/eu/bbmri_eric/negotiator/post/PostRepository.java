package eu.bbmri_eric.negotiator.post;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, String> {

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationId(String negotiationId);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndType(String negotiationId, PostType type);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndOrganizationId(String negotiationId, Long organizationId);

  @EntityGraph("post-with-details")
  Post findByIdAndNegotiationId(String negotiationId, String id);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndTypeAndOrganization_ExternalId(
      String negotiationId, PostType type, String organizationId);
}
