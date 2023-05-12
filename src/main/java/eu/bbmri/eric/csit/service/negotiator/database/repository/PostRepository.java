package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

  Optional<Post> findByPosterId(String id);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "WHERE n.id = :negotiationId")
  List<Post> findByNegotiationId(String negotiationId);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "WHERE n.id = :negotiationId and "
              + "p.id = :messageId"
  )
  Post findByNegotiationIdAndMessageId(String negotiationId, String messageId);

}
