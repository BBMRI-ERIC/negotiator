package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceNotFoundException;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceCreateModelMapper;
import eu.bbmri_eric.negotiator.governance.resource.ResourceModelMapper;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.ResourceServiceImpl;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceUpdateDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithOrgDTO;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

public class ResourceServiceTest {

  @Mock ResourceRepository resourceRepository;

  @Mock OrganizationRepository organizationRepository;

  @Mock NetworkRepository networkRepository;

  @Mock NegotiationRepository negotiationRepository;

  @Mock DiscoveryServiceRepository discoveryServiceRepository;

  @Mock AccessFormRepository accessFormRepository;

  @Spy ModelMapper modelMapper = new ModelMapper();

  @InjectMocks ResourceServiceImpl resourceService;

  private final Organization currentOrg = organization(10L);
  private Resource resource;
  private AutoCloseable closeable;

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
    new ResourceModelMapper(modelMapper).addMappings();
    ResourceCreateModelMapper createModelMapper = new ResourceCreateModelMapper();
    ReflectionTestUtils.setField(createModelMapper, "modelMapper", modelMapper);
    createModelMapper.addMappings();
    resource =
        Resource.builder().id(1L).sourceId("res").name("Original").organization(currentOrg).build();
    when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
    when(resourceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    when(resourceRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  private static Organization organization(Long id) {
    return Organization.builder()
        .id(id)
        .externalId("org" + id)
        .name("org" + id)
        .description("org" + id)
        .build();
  }

  private static ResourceCreateDTO createDTO(Long accessFormId) {
    return ResourceCreateDTO.builder()
        .name("New resource")
        .description("New resource")
        .sourceId("res:new")
        .organizationId(10L)
        .discoveryServiceId(1L)
        .accessFormId(accessFormId)
        .build();
  }

  private Resource savedResource() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Resource>> captor = ArgumentCaptor.forClass(List.class);
    verify(resourceRepository).saveAll(captor.capture());
    return captor.getValue().get(0);
  }

  @Test
  void findById_existingResource_returnsResourceWithOrganization() {
    ResourceWithOrgDTO result =
        assertInstanceOf(ResourceWithOrgDTO.class, resourceService.findById(1L));

    assertEquals("res", result.getSourceId());
    assertEquals(10L, result.getOrganization().getId());
  }

  @Test
  void findById_unknownResource_throwsEntityNotFound() {
    when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> resourceService.findById(99L));
  }

  @Test
  void findAllForNetwork_unknownNetwork_throwsEntityNotFound() {
    when(networkRepository.findById(5L)).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () -> resourceService.findAllForNetwork(Pageable.unpaged(), 5L));
  }

  @Test
  void findAllInNegotiation_unknownNegotiation_throwsEntityNotFound() {
    when(negotiationRepository.existsById("neg-1")).thenReturn(false);

    assertThrows(
        EntityNotFoundException.class, () -> resourceService.findAllInNegotiation("neg-1"));

    verify(resourceRepository, never()).findByNegotiation(any());
  }

  @Test
  void updateResourceById_newOrganization_movesResourceWithoutOverwritingIds() {
    Organization newOrg = organization(20L);
    when(organizationRepository.findById(20L)).thenReturn(Optional.of(newOrg));

    resourceService.updateResourceById(1L, ResourceUpdateDTO.builder().organizationId(20L).build());

    assertSame(newOrg, resource.getOrganization());
    assertEquals(1L, resource.getId());
    assertEquals(10L, currentOrg.getId());
  }

  @Test
  void updateResourceById_unknownOrganization_throwsAndDoesNotSave() {
    when(organizationRepository.findById(20L)).thenReturn(Optional.empty());
    ResourceUpdateDTO dto = ResourceUpdateDTO.builder().name("Ignored").organizationId(20L).build();

    assertThrows(EntityNotFoundException.class, () -> resourceService.updateResourceById(1L, dto));

    verify(resourceRepository, never()).saveAndFlush(any());
    assertEquals("Original", resource.getName());
  }

  @Test
  void addResources_noAccessFormId_usesMostCommonFormOfOrganization() {
    AccessForm commonForm = new AccessForm("common");
    DiscoveryService discoveryService = DiscoveryService.builder().id(1L).build();
    when(discoveryServiceRepository.findById(1L)).thenReturn(Optional.of(discoveryService));
    when(organizationRepository.findById(10L)).thenReturn(Optional.of(currentOrg));
    when(accessFormRepository.findFirstMostCommonAccessFormByOrganization(10L))
        .thenReturn(Optional.of(commonForm));

    resourceService.addResources(List.of(createDTO(null)));

    Resource saved = savedResource();
    assertSame(commonForm, saved.getAccessForm());
    assertSame(currentOrg, saved.getOrganization());
    assertSame(discoveryService, saved.getDiscoveryService());
  }

  @Test
  void addResources_noAccessFormIdAndNoCommonForm_usesDefaultForm() {
    AccessForm defaultForm = new AccessForm("default");
    when(discoveryServiceRepository.findById(1L))
        .thenReturn(Optional.of(DiscoveryService.builder().id(1L).build()));
    when(organizationRepository.findById(10L)).thenReturn(Optional.of(currentOrg));
    when(accessFormRepository.findFirstMostCommonAccessFormByOrganization(10L))
        .thenReturn(Optional.empty());
    when(accessFormRepository.findById(1L)).thenReturn(Optional.of(defaultForm));

    resourceService.addResources(List.of(createDTO(null)));

    assertSame(defaultForm, savedResource().getAccessForm());
  }

  @Test
  void addResources_unknownDiscoveryService_throwsAndSavesNothing() {
    when(discoveryServiceRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(
        DiscoveryServiceNotFoundException.class,
        () -> resourceService.addResources(List.of(createDTO(null))));

    verify(resourceRepository, never()).saveAll(anyList());
  }
}
