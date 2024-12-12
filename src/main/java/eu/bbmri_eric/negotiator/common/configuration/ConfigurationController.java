package eu.bbmri_eric.negotiator.common.configuration;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@Tag(name = "Configuration", description = "get and update configuration")
@SecurityRequirement(name = "security_auth")
public class ConfigurationController {}
