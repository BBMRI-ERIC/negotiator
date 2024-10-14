package eu.bbmri_eric.negotiator.settings;

import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service(value = "DefaultUIConfigService")
public class UIConfigServiceImpl implements UIConfigService {

  private final UIParameterRepository uiParameterRepository;

  public UIConfigServiceImpl(UIParameterRepository uiParameterRepository) {
    this.uiParameterRepository = uiParameterRepository;
  }

  @Override
  public Map<String, Map<String, Object>> getAllParameters() {
    List<UIParameter> parameters = uiParameterRepository.findAll();

    Map<String, Map<String, Object>> settings = new HashMap<>();

    parameters.forEach(
        uiParameter -> {
          switch (uiParameter.getType()) {
            case INT:
              settings
                  .computeIfAbsent(uiParameter.getCategory(), k -> new HashMap<>())
                  .put(uiParameter.getName(), Integer.valueOf(uiParameter.getValue()));
              break;
            case BOOL:
              settings
                  .computeIfAbsent(uiParameter.getCategory(), k -> new HashMap<>())
                  .put(uiParameter.getName(), Boolean.valueOf(uiParameter.getValue()));
              break;
            default:
              settings
                  .computeIfAbsent(uiParameter.getCategory(), k -> new HashMap<>())
                  .put(uiParameter.getName(), uiParameter.getValue());
              break;
          }
        });
    return settings;
  }

  @Override
  public void updateParameters(Map<String, Map<String, Object>> categories) {
    categories.forEach(
        (category, parameters) -> {
          parameters.forEach(
              (name, value) -> {
                UIParameter uiParameter =
                    uiParameterRepository.findByCategoryAndName(category, name);
                String stringValue = String.valueOf(value);
                switch (uiParameter.getType()) {
                  case INT:
                    try {
                      Integer.valueOf(stringValue);
                    } catch (NumberFormatException ex) {
                      throw new WrongRequestException(
                          "Value %s cannot be saved as INT".formatted(uiParameter.getValue()));
                    }
                    break;
                  case BOOL:
                    String lower = stringValue.toLowerCase();
                    if (!lower.equals("false") && !lower.equals("true")) {
                      throw new WrongRequestException(
                          "Value %s cannot be saved as BOOL".formatted(uiParameter.getValue()));
                    }
                    break;
                  default:
                    break;
                }
                uiParameter.setValue(stringValue);
                uiParameterRepository.save(uiParameter);
              });
        });
  }
}
