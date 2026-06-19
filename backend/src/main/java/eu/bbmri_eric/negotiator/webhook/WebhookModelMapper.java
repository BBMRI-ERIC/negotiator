package eu.bbmri_eric.negotiator.webhook;

import jakarta.annotation.PostConstruct;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WebhookModelMapper {

  private final ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<WebhookCreateDTO, Webhook> typeMap =
        Objects.requireNonNullElseGet(
            modelMapper.getTypeMap(WebhookCreateDTO.class, Webhook.class),
            () -> modelMapper.createTypeMap(WebhookCreateDTO.class, Webhook.class));
    typeMap.addMappings(
        mapper -> {
          mapper.skip(Webhook::setId);
          mapper.skip(Webhook::setSecretId);
        });
  }
}
