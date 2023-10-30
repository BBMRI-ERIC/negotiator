package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, String> {

  @EntityGraph(value = "attachment-metadata")
  Optional<Attachment> findMetadataById(String id);

  @EntityGraph(value = "attachment-metadata")
  List<Attachment> findByNegotiationId(String negotiationId);

  @EntityGraph(value = "attachment-metadata")
  Optional<Attachment> findByIdAndNegotiationId(String id, String negotiationId);
}
