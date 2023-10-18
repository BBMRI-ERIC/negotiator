package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostStatus;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

  Optional<Post> findByCreatedBy(Long id);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationId(String negotiationId);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndType(String negotiationId, PostType type);

  @EntityGraph("post-with-details")
  Post findByIdAndNegotiationId(String negotiationId, String id);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndStatusAndCreatedBy_authNameIn(
      String negotiationId, PostStatus status, List<String> authors);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndStatusAndTypeAndCreatedBy_authNameIn(
      String negotiationId, PostStatus status, PostType type, List<String> authors);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndTypeAndAndStatusAndCreatedBy_authNameInAndOrganization_ExternalId(
      String negotiationId, PostType type, PostStatus status, List<String> posters, String organizationId);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndTypeAndOrganization_ExternalId(
      String negotiationId, PostType type, String organizationId);
}
