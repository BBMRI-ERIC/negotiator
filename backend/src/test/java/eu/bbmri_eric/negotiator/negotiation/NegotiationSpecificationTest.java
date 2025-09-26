package eu.bbmri_eric.negotiator.negotiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;


/**
 * Unit tests for NegotiationSpecification.
 */
public class NegotiationSpecificationTest {

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
