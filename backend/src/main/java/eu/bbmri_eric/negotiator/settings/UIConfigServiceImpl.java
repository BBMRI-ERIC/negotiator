package eu.bbmri_eric.negotiator.settings;

import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
          settings
              .computeIfAbsent(uiParameter.getCategory(), k -> new HashMap<>())
              .put(uiParameter.getName(), uiParameter.getTypedValue());
        });
    return settings;
  }

  @Override
  public void updateParameters(Map<String, Map<String, Object>> categories) {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    categories.forEach(
        (category, parameters) -> {
          parameters.forEach(
              (name, value) -> {
                UIParameter uiParameter =
                    uiParameterRepository.findByCategoryAndName(category, name);

                String stringValue = String.valueOf(value);
                if (uiParameter.getType().equals(UIParameterType.BOOL)) {
                  stringValue = stringValue.toLowerCase();
                }
                uiParameter.setValue(stringValue);

                Set<ConstraintViolation<UIParameter>> validationErrors =
                    validator.validate(uiParameter);

                if (!validationErrors.isEmpty()) {
                  throw new WrongRequestException(
                      "The value '%s' for parameter '%s' of category '%s' cannot be applied to type %s"
                          .formatted(
                              uiParameter.getValue(),
                              uiParameter.getName(),
                              uiParameter.getCategory(),
                              uiParameter.getType()));
                }

                uiParameterRepository.save(uiParameter);
              });
        });
  }
}
