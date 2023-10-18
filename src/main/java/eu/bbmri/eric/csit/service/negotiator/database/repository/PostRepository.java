package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

  Optional<Post> findByCreatedBy(Long id);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationId(String negotiationId);

  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndType(String negotiationId, PostType type);

  //  @Query(
  //      value =
  //          "SELECT p "
  //              + "FROM Post p "
  //              + "JOIN FETCH p.negotiation n "
  //              + "WHERE n.id = :negotiationId and "
  //              + "p.id = :id")
  @EntityGraph("post-with-details")
  Post findByNegotiationIdAndId(String negotiationId, String id);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "WHERE n.id = :negotiationId and "
              + "p.createdBy.authName in :posters and "
              + "p.status = 'CREATED' ")
  //  @EntityGraph("post-with-details")
  List<Post> findNewByNegotiationIdAndPosters(String negotiationId, List<String> posters);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "WHERE n.id = :negotiationId and "
              + "p.createdBy.authName in :posters and "
              + "p.status = 'CREATED' and "
              + "p.type = :type ")
  //  @EntityGraph("post-with-details")
  List<Post> findNewByNegotiationIdAndPostersAndType(
      String negotiationId, List<String> posters, PostType type);

  @Query(
      value =
          "SELECT p "
              + "FROM Post p "
              + "JOIN FETCH p.negotiation n "
              + "JOIN FETCH p.organization o "
              + "WHERE n.id = :negotiationId and o.id = p.organization and "
              + "p.createdBy.authName in :posters and "
              + "p.status = 'CREATED' and "
              + "p.type = :type and "
              + "o.externalId = :organizationId")
  //  @EntityGraph("post-with-details")
  List<Post> findNewByNegotiationIdAndPostersAndTypeAndOrganizationId(
      String negotiationId, List<String> posters, PostType type, String organizationId);

    @Query(
        value =
            "SELECT p "
                + "FROM Post p "
                + "JOIN FETCH p.negotiation n "
                + "JOIN FETCH p.organization r "
                + "WHERE n.id = :negotiationId and r.id = p.organization and "
                + "p.type = :type and "
                + "r.externalId = :organizationId")
//  @EntityGraph("post-with-details")
  List<Post> findByNegotiationIdAndTypeAndOrganization(
      String negotiationId, PostType type, String organizationId);
}
