package eu.bbmri_eric.negotiator.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UIParameterRepository extends JpaRepository<UIParameter, Long> {}
