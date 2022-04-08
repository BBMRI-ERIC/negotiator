package eu.bbmri.eric.csit.service.repository;

import eu.bbmri.eric.csit.service.model.DataSource;
import java.util.Optional;
import javax.xml.crypto.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

  Optional<DataSource> findByUrl(String url);
}
