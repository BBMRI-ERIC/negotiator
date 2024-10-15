package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.settings.UIConfigServiceImpl;
import eu.bbmri_eric.negotiator.settings.UIParameter;
import eu.bbmri_eric.negotiator.settings.UIParameterRepository;
import eu.bbmri_eric.negotiator.settings.UIParameterType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@CommonsLog
public class UIConfigServiceTest {

  @Mock UIParameterRepository uiParameterRepository;

  @InjectMocks UIConfigServiceImpl uiConfigService;

  @BeforeEach
  void before() {
    MockitoAnnotations.openMocks(this);
    uiConfigService = new UIConfigServiceImpl(uiParameterRepository);
  }

  @Test
  void testGetAllParameters_Ok() {
    ArrayList<UIParameter> parameters = new ArrayList<>();
    parameters.add(new UIParameter(1L, "cat1", "anInt", UIParameterType.INT, "10"));
    parameters.add(new UIParameter(2L, "cat1", "aBool", UIParameterType.BOOL, "true"));
    parameters.add(new UIParameter(3L, "cat2", "aString", UIParameterType.STRING, "string"));
    when(uiParameterRepository.findAll()).thenReturn(parameters);

    Map<String, Map<String, Object>> newParameters = uiConfigService.getAllParameters();
    assertTrue(newParameters.containsKey("cat1"));
    assertTrue(newParameters.containsKey("cat2"));
    assertTrue(newParameters.get("cat1").containsKey("anInt"));
    assertTrue(newParameters.get("cat1").containsKey("aBool"));
    assertTrue(newParameters.get("cat2").containsKey("aString"));
    assertEquals(newParameters.get("cat1").get("anInt"), 10);
    assertEquals(newParameters.get("cat1").get("aBool"), true);
    assertEquals(newParameters.get("cat2").get("aString"), "string");
  }

  @Test
  void testUpdateParameters_Ok() {
    when(uiParameterRepository.findByCategoryAndName("cat1", "anInt"))
        .thenReturn(new UIParameter(1L, "cat1", "anInt", UIParameterType.INT, "10"));
    when(uiParameterRepository.findByCategoryAndName("cat1", "aBool"))
        .thenReturn(new UIParameter(2L, "cat1", "aBool", UIParameterType.BOOL, "true"));
    when(uiParameterRepository.findByCategoryAndName("cat2", "aString"))
        .thenReturn(new UIParameter(3L, "cat2", "aString", UIParameterType.STRING, "string"));

    Map<String, Map<String, Object>> updates = new HashMap<>();
    Map<String, Object> cat1Update = new HashMap<>();
    cat1Update.put("anInt", "12");
    cat1Update.put("aBool", "false");

    Map<String, Object> cat2Update = new HashMap<>();
    cat2Update.put("aString", "newValue");

    updates.put("cat1", cat1Update);
    updates.put("cat2", cat2Update);

    uiConfigService.updateParameters(updates);

    verify(uiParameterRepository, times(3)).save(any());
    verify(uiParameterRepository).save(argThat(parameter -> parameter.getValue().equals("12")));
    verify(uiParameterRepository).save(argThat(parameter -> parameter.getValue().equals("false")));
    verify(uiParameterRepository)
        .save(argThat(parameter -> parameter.getValue().equals("newValue")));
  }

  @Test
  void testUpdateParameters_raiseWrongRequest_whenWrongInt() {
    when(uiParameterRepository.findByCategoryAndName("cat1", "anInt"))
        .thenReturn(new UIParameter(1L, "cat1", "anInt", UIParameterType.INT, "10"));

    Map<String, Map<String, Object>> updates = new HashMap<>();
    Map<String, Object> cat1Update = new HashMap<>();
    cat1Update.put("anInt", "wrong_value");
    updates.put("cat1", cat1Update);

    assertThrows(WrongRequestException.class, () -> uiConfigService.updateParameters(updates));
  }

  @Test
  void testUpdateParameters_raiseWrongRequest_whenWrongBoll() {
    when(uiParameterRepository.findByCategoryAndName("cat1", "aBool"))
        .thenReturn(new UIParameter(1L, "cat1", "aBool", UIParameterType.BOOL, "true"));

    Map<String, Map<String, Object>> updates = new HashMap<>();
    Map<String, Object> cat1Update = new HashMap<>();
    cat1Update.put("aBool", "wrong_value");
    updates.put("cat1", cat1Update);

    assertThrows(WrongRequestException.class, () -> uiConfigService.updateParameters(updates));
  }
}
