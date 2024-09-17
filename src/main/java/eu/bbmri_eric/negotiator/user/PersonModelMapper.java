package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import java.util.Set;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonModelMapper {

  ModelMapper modelMapper;

  public PersonModelMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    TypeMap<Person, UserResponseModel> typeMap =
        modelMapper.createTypeMap(Person.class, UserResponseModel.class);

    typeMap.addMappings(mapper -> mapper.map(Person::getId, UserResponseModel::setId));
    typeMap.addMappings(
        mapper -> mapper.map(Person::getSubjectId, UserResponseModel::setSubjectId));
    Converter<Set<Resource>, Boolean> resourceToResourceModelConverter =
        q -> convertResourceToResourceModel(q.getSource());
    Converter<Set<Network>, Boolean> networkConverter = q -> convertNetworksModel(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(resourceToResourceModelConverter)
                .map(Person::getResources, UserResponseModel::setRepresentativeOfAnyResource));
    typeMap.addMappings(
        mapper ->
            mapper
                .using(networkConverter)
                .map(Person::getNetworks, UserResponseModel::setNetworkManager));
  }

  boolean convertResourceToResourceModel(Set<Resource> resources) {
    return Objects.nonNull(resources) && !resources.isEmpty();
  }

  boolean convertNetworksModel(Set<Network> networks) {
    return Objects.nonNull(networks) && !networks.isEmpty();
  }
}
