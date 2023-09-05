package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

  Optional<Post> findByCreatedBy(Long id);

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
              + "p.type = :type ")
  List<Post> findByNegotiationIdAndType(String negotiationId, Optional<PostType> type);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "WHERE n.id = :negotiationId and "
              + "p.id = :messageId")
  Post findByNegotiationIdAndMessageId(String negotiationId, String messageId);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "WHERE n.id = :negotiationId and "
              + "p.createdBy.authName in :posters and "
              + "p.status = 'CREATED' ")
  List<Post> findNewByNegotiationIdAndPosters(String negotiationId, List posters);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "WHERE n.id = :negotiationId and "
              + "p.createdBy.authName in :posters and "
              + "p.status = 'CREATED' and "
              + "p.type = :type ")
  List<Post> findNewByNegotiationIdAndPostersAndType(
      String negotiationId, List posters, Optional<PostType> type);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "JOIN FETCH p.resource r "
              + "WHERE n.id = :negotiationId and r.id = p.resource and "
              + "p.createdBy.authName in :posters and "
              + "p.status = 'CREATED' and "
              + "p.type = :type and "
              + "r.sourceId = :resourceId")
  List<Post> findNewByNegotiationIdAndPostersAndTypeAndResource(
      String negotiationId, List posters, Optional<PostType> type, Optional<String> resourceId);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "JOIN FETCH p.resource r "
              + "WHERE n.id = :negotiationId and r.id = p.resource and "
              + "p.type = :type and "
              + "r.sourceId = :resourceId")
  List<Post> findByNegotiationIdAndTypeAndResource(
      String negotiationId, Optional<PostType> type, Optional<String> resourceId);
}
