package eu.bbmri_eric.negotiator.settings;

import java.util.List;
import java.util.Map;

public interface UIConfigService {
  Map<String, List<Map<String, Object>>> getAllParameters();
}
