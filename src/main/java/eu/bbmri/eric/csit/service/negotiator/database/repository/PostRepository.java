package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostStatus;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostType;
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
  List<Post> findByNegotiationIdAndStatusAndCreatedBy_NameIn(
      String negotiationId, PostStatus status, List<String> authors);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameIn(
      String negotiationId, PostStatus status, PostType type, List<String> authors);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndStatusAndCreatedBy_NameInAndOrganization_ExternalId(
      String negotiationId, PostStatus status, List<String> authors, String organizationId);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
      String negotiationId,
      PostStatus status,
      PostType type,
      List<String> authors,
      String organizationId);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndTypeAndOrganization_ExternalId(
      String negotiationId, PostType type, String organizationId);
}
