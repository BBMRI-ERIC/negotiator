package eu.bbmri_eric.negotiator.post;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, String> {
  
  List<Post> findByNegotiationId(String negotiationId);
  
  Post findByIdAndNegotiationId(String negotiationId, String id);
  
  List<Post> findByNegotiationIdAndTypeAndOrganization_ExternalId(
      String negotiationId, PostType type, String organizationId);
}
