package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.settings.UIParameter;
import eu.bbmri_eric.negotiator.settings.UIParameterType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class UIParameterTest {

  @Test
  void initUIParameter_noArgsConstructor_ok() {
    new UIParameter();
  }

  @Test
  void testIsValid_False_whenValueIsNotCompatibleWithTheType() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    UIParameter uiParameterBool =
        new UIParameter(1L, "testCategory", "testNamee", UIParameterType.BOOL, "no_bool");
    Set<ConstraintViolation<UIParameter>> validations = validator.validate(uiParameterBool);
    assertEquals(1, validations.size());
    assertEquals("Invalid value for type", validations.stream().findFirst().get().getMessage());

    UIParameter uiParameterInt =
        new UIParameter(1L, "testCategory", "testNamee", UIParameterType.INT, "no_int");

    validations = validator.validate(uiParameterInt);
    assertEquals(1, validations.size());
    assertEquals("Invalid value for type", validations.stream().findFirst().get().getMessage());
  }

  @Test
  void testIsValid_True_whenValueIsCompatibleWithTheType() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    UIParameter uiParameterTrue =
        new UIParameter(1L, "testCategory", "testNamee", UIParameterType.BOOL, "true");
    UIParameter uiParameterFalse =
        new UIParameter(1L, "testCategory", "testNamee", UIParameterType.BOOL, "false");
    UIParameter uiParameterInt =
        new UIParameter(1L, "testCategory", "testNamee", UIParameterType.INT, "11");
    UIParameter uiParameterStr =
        new UIParameter(1L, "testCategory", "testNamee", UIParameterType.STRING, "anystring");
    Set<ConstraintViolation<UIParameter>> validations = validator.validate(uiParameterTrue);
    assertTrue(validations.isEmpty());
    validations = validator.validate(uiParameterFalse);
    assertTrue(validations.isEmpty());
    validations = validator.validate(uiParameterInt);
    assertTrue(validations.isEmpty());
    validations = validator.validate(uiParameterStr);
    assertTrue(validations.isEmpty());
  }

  @Test
  void test_getTypedValue() {
    assertInstanceOf(
        Boolean.class,
        (new UIParameter(1L, "testCategory", "testNamee", UIParameterType.BOOL, "true"))
            .getTypedValue());

    assertInstanceOf(
        Integer.class,
        (new UIParameter(1L, "testCategory", "testNamee", UIParameterType.INT, "11"))
            .getTypedValue());

    assertInstanceOf(
        String.class,
        (new UIParameter(1L, "testCategory", "testNamee", UIParameterType.STRING, "anystring"))
            .getTypedValue());
  }
}
