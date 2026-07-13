package eu.bbmri_eric.negotiator.lifecycle.resource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionPreconditionException;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InfoRequirementMetPreconditionTest {

  @Mock InformationRequirementRepository requirementRepository;
  @Mock InformationSubmissionRepository informationSubmissionRepository;
  private InfoRequirementMetPrecondition precondition;

  @BeforeEach
  void setUp() {
    precondition =
        new InfoRequirementMetPrecondition(requirementRepository, informationSubmissionRepository);
  }

  @Test
  void check_requirementMet_doesNotThrow() {
    when(requirementRepository.existsByForEvent(NegotiationResourceEvent.CONTACT))
        .thenReturn(true);
    when(informationSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
            "biobank:1:collection:1", "neg-1"))
        .thenReturn(true);

    assertThatCode(() -> precondition.check(ctx(), "CONTACT")).doesNotThrowAnyException();
  }

  @Test
  void check_requirementNotMet_throwsPreconditionException() {
    when(requirementRepository.existsByForEvent(NegotiationResourceEvent.CONTACT))
        .thenReturn(true);
    when(informationSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
            "biobank:1:collection:1", "neg-1"))
        .thenReturn(false);

    assertThatThrownBy(() -> precondition.check(ctx(), "CONTACT"))
        .isInstanceOf(TransitionPreconditionException.class);
  }

  @Test
  void check_noRequirementForEvent_doesNotThrow() {
    when(requirementRepository.existsByForEvent(NegotiationResourceEvent.CONTACT))
        .thenReturn(false);

    assertThatCode(() -> precondition.check(ctx(), "CONTACT")).doesNotThrowAnyException();
  }

  private ResourceTransitionContext ctx() {
    return new ResourceTransitionContext(
        "neg-1", Set.of(), "biobank:1:collection:1", 42L);
  }
}
