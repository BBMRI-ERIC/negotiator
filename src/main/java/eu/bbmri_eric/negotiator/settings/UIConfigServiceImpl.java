package eu.bbmri_eric.negotiator.settings;

import eu.bbmri_eric.negotiator.settings.dto.UISettingsDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service()
public class UIConfigServiceImpl implements UIConfigService {

  private final UIParameterRepository uiParameterRepository;

  public UIConfigServiceImpl(UIParameterRepository uiParameterRepository) {
    this.uiParameterRepository = uiParameterRepository;
  }

  @Override
  public Map<String, List<Map<String, Object>>> getAllParameters() {
    List<UIParameter> parameters = uiParameterRepository.findAll();
    UISettingsDTO uiSettingsDTO = new UISettingsDTO();

    Map<String, List<Map<String, Object>>> settings = new HashMap<>();

    parameters.forEach(
        uiParameter -> {
          Map<String, Object> parameter = new HashMap<>();
          switch (uiParameter.type) {
            case INT:
              parameter.put(uiParameter.getName(), Integer.valueOf(uiParameter.getValue()));
              break;
            case BOOL:
              parameter.put(uiParameter.getName(), Boolean.valueOf(uiParameter.getValue()));
              break;
            default:
              parameter.put(uiParameter.getName(), uiParameter.getValue());
              break;
          }
          settings
              .computeIfAbsent(uiParameter.getCategory(), k -> new ArrayList<>())
              .add(parameter);
        });
    return settings;
  }
}
