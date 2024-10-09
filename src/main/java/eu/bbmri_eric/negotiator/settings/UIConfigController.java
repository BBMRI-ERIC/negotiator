package eu.bbmri_eric.negotiator.settings;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@Tag(name = "Settings", description = "Get setting")
public class UIConfigController {

  UIConfigService uiConfigService;

  public UIConfigController(UIConfigService uiConfigService) {
    this.uiConfigService = uiConfigService;
  }

  @GetMapping(value = "/ui-config", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public Map<String, List<Map<String, Object>>> getUIConfiguration() {
    return uiConfigService.getAllParameters();
  }
}
