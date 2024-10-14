package eu.bbmri_eric.negotiator.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  public Map<String, Map<String, Object>> getUIConfiguration() {
    return uiConfigService.getAllParameters();
  }

  @PutMapping(value = "/ui-config", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Update UI Config parameters")
  @SecurityRequirement(name = "security_auth")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateUIConfiguration(
      @RequestBody @Valid Map<String, Map<String, Object>> parameters) {
    uiConfigService.updateParameters(parameters);
  }
}
