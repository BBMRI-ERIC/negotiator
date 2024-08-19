package eu.bbmri_eric.negotiator.info_requirement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InformationSubmissionMapper {
  ModelMapper modelMapper;

  public InformationSubmissionMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    TypeMap<InformationSubmission, SubmittedInformationDTO> typeMap =
        modelMapper.createTypeMap(InformationSubmission.class, SubmittedInformationDTO.class);
    Converter<String, JsonNode> payloadConverter =
        p -> {
          try {
            return payloadConverter(p.getSource());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e); // TODO: raise the correct exception
          }
        };
    typeMap.addMappings(
        mapper ->
            mapper
                .using(payloadConverter)
                .map(InformationSubmission::getPayload, SubmittedInformationDTO::setPayload));
  }

  private JsonNode payloadConverter(String jsonPayload) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    if (jsonPayload == null) {
      jsonPayload = "{}";
    }
    return mapper.readTree(jsonPayload);
  }
}
