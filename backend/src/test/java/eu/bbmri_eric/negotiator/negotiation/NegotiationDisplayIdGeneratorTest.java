package eu.bbmri_eric.negotiator.negotiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

class NegotiationDisplayIdGeneratorTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @InjectMocks private NegotiationDisplayIdGenerator generator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void generateDisplayIdReturnsNextSequenceValueAsString() {
    // Arrange
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(42);

    // Act
    String displayId = generator.generateDisplayId();

    // Assert
    assertEquals("42", displayId);
    verify(jdbcTemplate)
        .queryForObject("SELECT nextval('negotiation_display_id_seq')", Integer.class);
  }
}
