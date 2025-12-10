package eu.bbmri_eric.negotiator.negotiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationFilterDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.user.Person;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/** Unit tests for NegotiationSpecification. */
public class NegotiationSpecificationTest {

  private Person mockUser;
  private Network mockNetwork;
  private Resource mockResource;

  @BeforeEach
  void setUp() {
    mockUser = mock(Person.class);
    mockNetwork = mock(Network.class);
    mockResource = mock(Resource.class);
  }

  @Test
  void testFromNegotiationFilters_withNullUser_returnsNull() {
    // Arrange
    NegotiationFilterDTO filterDTO = NegotiationFilterDTO.builder().build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, null, null);

    // Assert
    assertNull(result);
  }

  @Test
  void testFromNegotiationFilters_withUserAndNullRole_returnsAuthorOrRepresentativeSpec() {
    // Arrange
    NegotiationFilterDTO filterDTO = NegotiationFilterDTO.builder().role(null).build();
    when(mockUser.getResources()).thenReturn(new HashSet<>());

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withAuthorRole_returnsAuthorSpec() {
    // Arrange
    NegotiationFilterDTO filterDTO =
        NegotiationFilterDTO.builder().role(NegotiationRole.AUTHOR).build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withRepresentativeRole_returnsResourcesAndNonDraftSpec() {
    // Arrange
    Set<Resource> resources = new HashSet<>();
    resources.add(mockResource);
    NegotiationFilterDTO filterDTO =
        NegotiationFilterDTO.builder().role(NegotiationRole.REPRESENTATIVE).build();
    when(mockUser.getResources()).thenReturn(resources);

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withStatusFilter_appliesStatusSpec() {
    // Arrange
    List<NegotiationState> statuses =
        Arrays.asList(NegotiationState.DRAFT, NegotiationState.SUBMITTED);
    NegotiationFilterDTO filterDTO = NegotiationFilterDTO.builder().status(statuses).build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withEmptyStatusFilter_ignoresStatus() {
    // Arrange
    NegotiationFilterDTO filterDTO =
        NegotiationFilterDTO.builder().status(Collections.emptyList()).build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withDateRangeFilter_appliesDateSpec() {
    // Arrange
    LocalDate createdAfter = LocalDate.of(2024, 1, 1);
    LocalDate createdBefore = LocalDate.of(2024, 12, 31);
    NegotiationFilterDTO filterDTO =
        NegotiationFilterDTO.builder()
            .createdAfter(createdAfter)
            .createdBefore(createdBefore)
            .build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withOnlyCreatedAfter_appliesDateSpec() {
    // Arrange
    LocalDate createdAfter = LocalDate.of(2024, 1, 1);
    NegotiationFilterDTO filterDTO =
        NegotiationFilterDTO.builder().createdAfter(createdAfter).build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withOnlyCreatedBefore_appliesDateSpec() {
    // Arrange
    LocalDate createdBefore = LocalDate.of(2024, 12, 31);
    NegotiationFilterDTO filterDTO =
        NegotiationFilterDTO.builder().createdBefore(createdBefore).build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withNetwork_appliesNetworkSpec() {
    // Arrange
    NegotiationFilterDTO filterDTO = NegotiationFilterDTO.builder().build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, mockNetwork);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withOrganizationId_appliesOrganizationSpec() {
    // Arrange
    List<Long> organizationIds = Arrays.asList(1L, 2L);
    NegotiationFilterDTO filterDTO =
        NegotiationFilterDTO.builder().organizationId(organizationIds).build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withDisplayId_appliesDisplayIdSpec() {
    // Arrange
    String displayId = "TEST-NEG";
    NegotiationFilterDTO filterDTO = NegotiationFilterDTO.builder().displayId(displayId).build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withEmptyDisplayId_ignoresDisplayId() {
    // Arrange
    NegotiationFilterDTO filterDTO = NegotiationFilterDTO.builder().displayId("").build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withWhitespaceDisplayId_ignoresDisplayId() {
    // Arrange
    NegotiationFilterDTO filterDTO = NegotiationFilterDTO.builder().displayId("   ").build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, null);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withMultipleFilters_combinesSpecs() {
    // Arrange
    List<NegotiationState> statuses = Arrays.asList(NegotiationState.SUBMITTED);
    List<Long> organizationIds = Arrays.asList(1L);
    LocalDate createdAfter = LocalDate.of(2024, 1, 1);
    String displayId = "TEST";

    NegotiationFilterDTO filterDTO =
        NegotiationFilterDTO.builder()
            .role(NegotiationRole.AUTHOR)
            .status(statuses)
            .organizationId(organizationIds)
            .createdAfter(createdAfter)
            .displayId(displayId)
            .build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, mockUser, mockNetwork);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testFromNegotiationFilters_withNoFiltersAndNoUser_returnsNull() {
    // Arrange
    NegotiationFilterDTO filterDTO = NegotiationFilterDTO.builder().build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, null, null);

    // Assert
    assertNull(result);
  }

  @Test
  void testFromNegotiationFilters_withOnlyNetworkAndNoUser_appliesNetworkSpec() {
    // Arrange
    NegotiationFilterDTO filterDTO = NegotiationFilterDTO.builder().build();

    // Act
    Specification<Negotiation> result =
        NegotiationSpecification.fromNegotiationFilters(filterDTO, null, mockNetwork);

    // Assert
    assertNotNull(result);
  }

  @Test
  void testByOrganization_withSingleOrganizationId_buildsEqualPredicate() {
    // Arrange
    List<Long> organizationIds = Arrays.asList(1L);

    @SuppressWarnings("unchecked")
    Root<Negotiation> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    @SuppressWarnings("unchecked")
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    @SuppressWarnings("unchecked")
    Predicate predicate = mock(Predicate.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.SetJoin<Object, Object> resourcesLinkJoin =
        mock(jakarta.persistence.criteria.SetJoin.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Join<Object, Object> idJoin =
        mock(jakarta.persistence.criteria.Join.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Join<Object, Object> resourceJoin =
        mock(jakarta.persistence.criteria.Join.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Join<Object, Object> organizationJoin =
        mock(jakarta.persistence.criteria.Join.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Path<Object> organizationIdPath =
        mock(jakarta.persistence.criteria.Path.class);

    // Mock the join chain
    when(root.joinSet(anyString())).thenReturn(resourcesLinkJoin);
    when(resourcesLinkJoin.join(anyString())).thenReturn(idJoin);
    when(idJoin.join(anyString())).thenReturn(resourceJoin);
    when(resourceJoin.join(anyString())).thenReturn(organizationJoin);
    when(organizationJoin.get(anyString())).thenReturn(organizationIdPath);
    when(cb.equal(organizationIdPath, 1L)).thenReturn(predicate);

    // Act
    Specification<Negotiation> spec = NegotiationSpecification.byOrganization(organizationIds);
    Predicate result = spec.toPredicate(root, query, cb);

    // Assert
    assertNotNull(result);
    assertEquals(predicate, result);
    verify(query).distinct(true);
    verify(cb).equal(organizationIdPath, 1L);
  }

  @Test
  void testByOrganization_withMultipleOrganizationIds_buildsInPredicate() {
    // Arrange
    List<Long> organizationIds = Arrays.asList(1L, 2L, 3L);

    @SuppressWarnings("unchecked")
    Root<Negotiation> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    @SuppressWarnings("unchecked")
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    @SuppressWarnings("unchecked")
    CriteriaBuilder.In<Object> inPredicate = mock(CriteriaBuilder.In.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.SetJoin<Object, Object> resourcesLinkJoin =
        mock(jakarta.persistence.criteria.SetJoin.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Join<Object, Object> idJoin =
        mock(jakarta.persistence.criteria.Join.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Join<Object, Object> resourceJoin =
        mock(jakarta.persistence.criteria.Join.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Join<Object, Object> organizationJoin =
        mock(jakarta.persistence.criteria.Join.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Path<Object> organizationIdPath =
        mock(jakarta.persistence.criteria.Path.class);

    // Mock the join chain
    when(root.joinSet(anyString())).thenReturn(resourcesLinkJoin);
    when(resourcesLinkJoin.join(anyString())).thenReturn(idJoin);
    when(idJoin.join(anyString())).thenReturn(resourceJoin);
    when(resourceJoin.join(anyString())).thenReturn(organizationJoin);
    when(organizationJoin.get(anyString())).thenReturn(organizationIdPath);
    when(cb.in(organizationIdPath)).thenReturn(inPredicate);
    when(inPredicate.value(organizationIds)).thenReturn(inPredicate);

    // Act
    Specification<Negotiation> spec = NegotiationSpecification.byOrganization(organizationIds);
    Predicate result = spec.toPredicate(root, query, cb);

    // Assert
    assertNotNull(result);
    assertEquals(inPredicate, result);
    verify(query).distinct(true);
    verify(cb).in(organizationIdPath);
    verify(inPredicate).value(organizationIds);
  }

  @Test
  public void testSearchByDisplayId_buildsCorrectPredicate() {
    // Arrange
    String searchTerm = "ABC";
    String expectedPattern = "%abc%";

    @SuppressWarnings("unchecked")
    Root<Negotiation> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    @SuppressWarnings("unchecked")
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    @SuppressWarnings("unchecked")
    Predicate predicate = mock(Predicate.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Path<String> displayIdPath =
        mock(jakarta.persistence.criteria.Path.class);
    @SuppressWarnings("unchecked")
    jakarta.persistence.criteria.Expression<String> lowerDisplayIdExpr =
        mock(jakarta.persistence.criteria.Expression.class);

    // Mock CriteriaBuilder and Root behavior
    // Use ArgumentMatchers to avoid generic type issues
    when(root.get(anyString())).thenReturn((jakarta.persistence.criteria.Path) displayIdPath);
    when(cb.lower(displayIdPath)).thenReturn(lowerDisplayIdExpr);
    when(cb.like(lowerDisplayIdExpr, expectedPattern)).thenReturn(predicate);

    // Act
    Specification<Negotiation> spec = NegotiationSpecification.searchByDisplayID(searchTerm);
    Predicate result = spec.toPredicate(root, query, cb);

    // Assert
    assertNotNull(result);
    assertEquals(predicate, result);
    verify(cb).like(lowerDisplayIdExpr, expectedPattern);
  }
}
