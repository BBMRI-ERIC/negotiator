package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.lifecycle.TransitionPreconditionException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.user.PersonService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.apachecommons.CommonsLog;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Flowable-backed implementation of the NegotiationResource lifecycle service. */
@Service("flowableResourceLifecycleServiceImpl")
@CommonsLog
@Transactional
public class ResourceLifecycleServiceImpl implements ResourceLifecycleService {

  private final NegotiationRepository negotiationRepository;
  private final InformationRequirementRepository requirementRepository;
  private final InformationSubmissionRepository requirementSubmissionRepository;
  private final PersonService personService;
  private final RuntimeService runtimeService;
  private final RepositoryService repositoryService;
  private final ApplicationEventPublisher eventPublisher;

  public ResourceLifecycleServiceImpl(
      NegotiationRepository negotiationRepository,
      InformationRequirementRepository requirementRepository,
      InformationSubmissionRepository requirementSubmissionRepository,
      PersonService personService,
      RuntimeService runtimeService,
      RepositoryService repositoryService,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.requirementRepository = requirementRepository;
    this.requirementSubmissionRepository = requirementSubmissionRepository;
    this.personService = personService;
    this.runtimeService = runtimeService;
    this.repositoryService = repositoryService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId) {
    NegotiationResourceState currentState;
    try {
      currentState = currentResourceState(negotiationId, resourceId);
    } catch (EntityNotFoundException e) {
      return Set.of();
    }
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    if (!negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      return Set.of();
    }
    ProcessInstance instance = findOrStartProcessInstance(negotiationId, resourceId, currentState);
    return possibleEventsFromState(
        instance, currentActivityId(instance), negotiationId, resourceId);
  }

  @Override
  public NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent event) {
    if (requirementRepository.existsByForEvent(event)
        && !requirementSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
            resourceId, negotiationId)) {
      throw new TransitionPreconditionException(
          "The requirement for this operation was not met. Please make sure you have submitted"
              + " the required form and try again.");
    }
    if (!getPossibleEvents(negotiationId, resourceId).contains(event)) {
      return currentResourceState(negotiationId, resourceId);
    }

    NegotiationResourceState fromState = currentResourceState(negotiationId, resourceId);
    ProcessInstance instance = findOrStartProcessInstance(negotiationId, resourceId, fromState);
    String toStateId = advance(instance, fromState.name(), event);
    NegotiationResourceState toState = NegotiationResourceState.valueOf(toStateId);

    persist(negotiationId, resourceId, fromState, toState, event);
    return toState;
  }

  @Override
  public Map<String, Object> getStateMachineDiagram() {
    ProcessDefinition definition =
        repositoryService
            .createProcessDefinitionQuery()
            .processDefinitionKey("resource")
            .latestVersion()
            .singleResult();
    BpmnModel model = repositoryService.getBpmnModel(definition.getId());
    Map<String, Object> diagram = new HashMap<>();
    traverseState("SUBMITTED", model, diagram);
    return diagram;
  }

  private void traverseState(String stateId, BpmnModel model, Map<String, Object> diagram) {
    FlowElement stateElement = model.getFlowElement(stateId);
    if (!(stateElement instanceof FlowNode)) {
      return;
    }
    Map<String, Object> transitions = new HashMap<>();
    for (SequenceFlow eventFlow : outgoingEventFlows(model, stateId)) {
      FlowElement catchEvent = model.getFlowElement(eventFlow.getTargetRef());
      String eventName = catchEvent.getName();
      String targetId = targetStateId(model, catchEvent.getId());
      Map<String, Object> transitionMap = new HashMap<>();
      transitionMap.put("target", targetId);
      transitionMap.put("event", eventName);
      transitions.put(eventName, transitionMap);
      traverseState(targetId, model, transitionMap);
    }
    if (!transitions.isEmpty()) {
      diagram.put(stateId, transitions);
    }
  }

  private void persist(
      String negotiationId,
      String resourceId,
      NegotiationResourceState fromState,
      NegotiationResourceState toState,
      NegotiationResourceEvent event) {
    Negotiation negotiation =
        negotiationRepository
            .findDetailedById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    negotiation.setStateForResource(resourceId, toState);
    negotiationRepository.saveAndFlush(negotiation);
    eventPublisher.publishEvent(
        new ResourceStateChangeEvent(this, negotiationId, resourceId, fromState, toState, event));
    log.debug(
        "Resource %s in negotiation %s transitioned to %s via %s"
            .formatted(resourceId, negotiationId, toState, event));
  }

  private NegotiationResourceState currentResourceState(String negotiationId, String resourceId) {
    return negotiationRepository
        .findNegotiationResourceStateById(negotiationId, resourceId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private ProcessInstance findOrStartProcessInstance(
      String negotiationId, String resourceId, NegotiationResourceState currentState) {
    String businessKey = businessKey(negotiationId, resourceId);
    ProcessInstance instance =
        runtimeService
            .createProcessInstanceQuery()
            .processInstanceBusinessKey(businessKey)
            .singleResult();
    if (instance != null) {
      return instance;
    }
    return runtimeService.startProcessInstanceByKey(
        "resource",
        businessKey,
        Map.of(
            "negotiationId", negotiationId,
            "resourceId", resourceId,
            "initialState", currentState.name()));
  }

  private String businessKey(String negotiationId, String resourceId) {
    return negotiationId + ":" + resourceId;
  }

  private String currentActivityId(ProcessInstance instance) {
    return runtimeService.getActiveActivityIds(instance.getId()).get(0);
  }

  /**
   * Advances the process past the current state task, through the message correlating to {@code
   * event}, and returns the resulting state's id, derived statically from the BPMN model (a
   * transition to a terminal state ends the process instance, so the runtime is not queryable
   * afterward).
   */
  private String advance(
      ProcessInstance instance, String fromStateId, NegotiationResourceEvent event) {
    BpmnModel model = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
    String catchEventId = catchEventIdForEvent(model, fromStateId, event);
    String toStateId = targetStateId(model, catchEventId);

    String stateExecutionId =
        runtimeService
            .createExecutionQuery()
            .processInstanceId(instance.getId())
            .activityId(fromStateId)
            .singleResult()
            .getId();
    runtimeService.trigger(stateExecutionId);

    String catchExecutionId =
        runtimeService
            .createExecutionQuery()
            .processInstanceId(instance.getId())
            .activityId(catchEventId)
            .singleResult()
            .getId();
    runtimeService.messageEventReceived(event.name(), catchExecutionId);
    return toStateId;
  }

  private String targetStateId(BpmnModel model, String catchEventId) {
    FlowNode node = (FlowNode) model.getFlowElement(catchEventId);
    while (true) {
      FlowElement target = model.getFlowElement(node.getOutgoingFlows().get(0).getTargetRef());
      if (target instanceof ReceiveTask || target instanceof EndEvent) {
        return target.getId();
      }
      node = (FlowNode) target;
    }
  }

  private Set<NegotiationResourceEvent> possibleEventsFromState(
      ProcessInstance instance, String currentStateId, String negotiationId, String resourceId) {
    BpmnModel model = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
    Set<NegotiationResourceEvent> events = new HashSet<>();
    for (SequenceFlow eventFlow : outgoingEventFlows(model, currentStateId)) {
      if (!isSecurityRuleMet(securityAttributes(eventFlow), negotiationId, resourceId)) {
        continue;
      }
      FlowElement catchEvent = model.getFlowElement(eventFlow.getTargetRef());
      events.add(NegotiationResourceEvent.valueOf(catchEvent.getName()));
    }
    return events;
  }

  private String catchEventIdForEvent(
      BpmnModel model, String currentStateId, NegotiationResourceEvent event) {
    for (SequenceFlow eventFlow : outgoingEventFlows(model, currentStateId)) {
      FlowElement catchEvent = model.getFlowElement(eventFlow.getTargetRef());
      if (catchEvent.getName().equals(event.name())) {
        return catchEvent.getId();
      }
    }
    throw new IllegalStateException(
        "No catch event for event %s from state %s".formatted(event, currentStateId));
  }

  private List<SequenceFlow> outgoingEventFlows(BpmnModel model, String currentStateId) {
    FlowElement stateTask = model.getFlowElement(currentStateId);
    SequenceFlow toGateway = ((FlowNode) stateTask).getOutgoingFlows().get(0);
    FlowElement gateway = model.getFlowElement(toGateway.getTargetRef());
    return ((FlowNode) gateway).getOutgoingFlows();
  }

  private Set<String> securityAttributes(SequenceFlow flow) {
    List<ExtensionElement> securityElements = flow.getExtensionElements().get("security");
    if (securityElements == null || securityElements.isEmpty()) {
      return Set.of();
    }
    List<ExtensionAttribute> rolesAttribute = securityElements.get(0).getAttributes().get("roles");
    if (rolesAttribute == null || rolesAttribute.isEmpty()) {
      return Set.of();
    }
    return Set.of(rolesAttribute.get(0).getValue().split(","));
  }

  private boolean isSecurityRuleMet(
      Set<String> attributes, String negotiationId, String resourceId) {
    if (attributes.isEmpty()) {
      return true;
    }
    Long creatorId;
    try {
      creatorId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    } catch (ClassCastException e) {
      return false;
    } catch (NullPointerException e) {
      creatorId = 0L;
    }
    if (attributes.contains("isCreator")) {
      return negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, creatorId);
    } else if (attributes.contains("isRepresentative")) {
      return personService.isRepresentativeOfAnyResource(
          AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), List.of(resourceId));
    } else if (attributes.contains("isAdmin")) {
      return Objects.isNull(SecurityContextHolder.getContext().getAuthentication())
          || AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin();
    }
    return true;
  }
}
