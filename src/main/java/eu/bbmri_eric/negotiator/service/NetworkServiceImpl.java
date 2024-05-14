package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.repository.NetworkRepository;
import eu.bbmri_eric.negotiator.dto.NetworkDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class NetworkServiceImpl implements NetworkService {
  @Autowired NetworkRepository networkRepository;

  ModelMapper modelMapper = new ModelMapper();

  @Override
  public NetworkDTO findNetworkById(Long id) {
    return modelMapper.map(
        networkRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        NetworkDTO.class);
  }

  @Override
  public Iterable<NetworkDTO> findAllNetworks(Pageable pageable) {
    return networkRepository
        .findAll(pageable)
        .map(network -> modelMapper.map(network, NetworkDTO.class));
  }
}
