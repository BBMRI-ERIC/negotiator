package eu.bbmri_eric.negotiator.template;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Template entities in the database. Provides CRUD operations and
 * query capabilities for database-stored templates.
 */
@Repository
interface TemplateRepository
    extends JpaRepository<Template, Long>, JpaSpecificationExecutor<Template> {

  /**
   * Find a template by its name.
   *
   * @param name the name of the template
   * @return Optional containing the template if found, empty otherwise
   */
  Optional<Template> findByName(String name);

  /**
   * Check if a template with the given name exists.
   *
   * @param name the name of the template
   * @return true if template exists, false otherwise
   */
  boolean existsByName(String name);

  /**
   * Delete a template by its name.
   *
   * @param name the name of the template to delete
   */
  void deleteByName(String name);
}
