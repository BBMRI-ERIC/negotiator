package eu.bbmri_eric.negotiator.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeliveryModelMapper {

  private final ModelMapper modelMapper;
  private final ObjectMapper objectMapper;

  public DeliveryModelMapper(ModelMapper modelMapper, ObjectMapper objectMapper) {
    this.modelMapper = modelMapper;
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  public void addMappings() {
    TypeMap<DeliveryDTO, Delivery> dtoToEntity =
        modelMapper.createTypeMap(DeliveryDTO.class, Delivery.class);
    Converter<JsonNode, String> jsonNodeToString =
        ctx -> ctx.getSource() == null ? null : ctx.getSource().toString();
    dtoToEntity.addMappings(
        mapper ->
            mapper.using(jsonNodeToString).map(DeliveryDTO::getContent, Delivery::setContent));
    dtoToEntity.addMappings(mapper -> mapper.skip(Delivery::setId));

    Converter<String, JsonNode> stringToJsonNode =
        ctx -> {
          if (ctx.getSource() == null) return null;
          try {
            return objectMapper.readTree(ctx.getSource());
          } catch (Exception ex) {
            return null;
          }
        };
    TypeMap<Delivery, DeliveryDTO> entityToDto =
        modelMapper.createTypeMap(Delivery.class, DeliveryDTO.class);

    entityToDto.addMappings(
        mapper -> {
          mapper.using(stringToJsonNode).map(Delivery::getContent, DeliveryDTO::setContent);
          // Skip auto-mapping for fields handled by the PostConverter to prevent ModelMapper from
          // incorrectly matching redeliveryOfDeliveryId (String) to redelivery (Boolean).
          mapper.skip(DeliveryDTO::setRootId);
          mapper.skip(DeliveryDTO::setRedelivery);
        });

    // Conditional logic for rootId and redelivery flag must use a PostConverter because
    // ModelMapper does not support conditional property access in mapper.map() lambdas.
    entityToDto.setPostConverter(
        ctx -> {
          Delivery src = ctx.getSource();
          DeliveryDTO dest = ctx.getDestination();
          String redeliveryId = src.getRedeliveryOfDeliveryId();
          dest.setRootId(redeliveryId != null ? redeliveryId : src.getId());
          dest.setRedelivery(redeliveryId != null);
          return dest;
        });

    entityToDto.validate();
  }
}
