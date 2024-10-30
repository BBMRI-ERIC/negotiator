package eu.bbmri_eric.negotiator.settings;

import java.util.Map;

public interface UIConfigService {
  Map<String, Map<String, Object>> getAllParameters();

  void updateParameters(Map<String, Map<String, Object>> parameters);
}
