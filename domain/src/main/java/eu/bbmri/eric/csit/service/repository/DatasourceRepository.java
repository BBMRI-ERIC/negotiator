package eu.bbmri.eric.csit.service.repository;

import eu.bbmri.eric.csit.service.model.Datasource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasourceRepository extends JpaRepository<Datasource, Long> {}
