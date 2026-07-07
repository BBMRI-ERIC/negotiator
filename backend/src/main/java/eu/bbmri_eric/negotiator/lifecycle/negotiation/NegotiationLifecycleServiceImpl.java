package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Flowable-backed implementation of the Negotiation lifecycle service. */
@Service("flowableNegotiationLifecycleServiceImpl")
@CommonsLog
@Transactional
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  private final NegotiationRepository negotiationRepository;
  private final PostRepository postRepository;
  private final PersonRepository personRepository;
  private final RuntimeService runtimeService;
  private final RepositoryService repositoryService;
  private final ApplicationEventPublisher eventPublisher;

  public NegotiationLifecycleServiceImpl(
      NegotiationRepository negotiationRepository,
      PostRepository postRepository,
      PersonRepository personRepository,
      RuntimeService runtimeService,
      RepositoryService repositoryService,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.postRepository = postRepository;
    this.personRepository = personRepository;
    this.runtimeService = runtimeService;
    this.repositoryService = repositoryService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Set<NegotiationEvent> getPossibleEvents(String negotiationId) {
    Long userId;
    try {
      userId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    } catch (ClassCastException e) {
      throw new ForbiddenRequestException("You are not allowed to perform this action");
    }
    List<String> roles = AuthenticatedUserContext.getRoles();
    if (!roles.contains("ROLE_ADMIN")
        && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userId)) {
      return Set.of();
    }
    ProcessInstance instance = findProcessInstance(negotiationId);
    return possibleEventsFromState(instance, currentActivityId(instance), roles);
  }

  @Override
  public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) {
    return sendEvent(negotiationId, negotiationEvent, null);
  }

  @Override
  public NegotiationState sendEvent(
      String negotiationId, NegotiationEvent negotiationEvent, String message) {
    if (!getPossibleEvents(negotiationId).contains(negotiationEvent)) {
      throw new ForbiddenRequestException(
          "You are not allowed to %s the Negotiation"
              .formatted(negotiationEvent.getLabel().toLowerCase()));
    }

    ProcessInstance instance = findProcessInstance(negotiationId);
    NegotiationState fromState = NegotiationState.valueOf(currentActivityId(instance));
    String toStateId = advance(instance, fromState.name(), negotiationEvent);
    NegotiationState toState = NegotiationState.valueOf(toStateId);

    Long senderId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    persist(negotiationId, toState, negotiationEvent, message, senderId);
    eventPublisher.publishEvent(
        new NegotiationStateChangeEvent(this, negotiationId, fromState, toState, negotiationEvent));
    return toState;
  }

  private void persist(
      String negotiationId,
      NegotiationState toState,
      NegotiationEvent event,
      String message,
      Long senderId) {
    Negotiation negotiation =
        negotiationRepository
            .findDetailedById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    negotiation.setCurrentState(toState);
    if (toState.equals(NegotiationState.SUBMITTED)) {
      negotiation.setCreationDate(LocalDateTime.now());
    }
    negotiationRepository.saveAndFlush(negotiation);
    if (message != null && !message.isEmpty()) {
      Person sender = personRepository.findById(senderId).orElse(null);
      Post post =
          Post.builder().negotiation(negotiation).text(message).type(PostType.PUBLIC).build();
      post.setCreatedBy(sender);
      post.setCreationDate(LocalDateTime.now());
      postRepository.save(post);
    }
    log.debug("Negotiation %s transitioned to %s via %s".formatted(negotiationId, toState, event));
  }

  private ProcessInstance findProcessInstance(String negotiationId) {
    ProcessInstance instance =
        runtimeService
            .createProcessInstanceQuery()
            .processInstanceBusinessKey(negotiationId)
            .singleResult();
    if (instance == null) {
      throw new EntityNotFoundException(negotiationId);
    }
    return instance;
  }

  private String currentActivityId(ProcessInstance instance) {
    return runtimeService.getActiveActivityIds(instance.getId()).get(0);
  }

  /**
   * Advances the process past the current state task, through the message correlating to {@code
   * event}, and returns the resulting state's id. The target state is derived statically from the
   * BPMN model (not re-queried from the runtime afterward) because a transition to a terminal state
   * ends the process instance, and its runtime execution is no longer queryable at that point.
   */
  private String advance(ProcessInstance instance, String fromStateId, NegotiationEvent event) {
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

  private Set<NegotiationEvent> possibleEventsFromState(
      ProcessInstance instance, String currentStateId, List<String> roles) {
    BpmnModel model = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
    Set<NegotiationEvent> events = new HashSet<>();
    for (SequenceFlow eventFlow : outgoingEventFlows(model, currentStateId)) {
      Set<String> requiredRoles = securityRoles(eventFlow);
      if (!requiredRoles.isEmpty() && requiredRoles.stream().noneMatch(roles::contains)) {
        continue;
      }
      FlowElement catchEvent = model.getFlowElement(eventFlow.getTargetRef());
      events.add(NegotiationEvent.valueOf(catchEvent.getName()));
    }
    return events;
  }

  private String catchEventIdForEvent(
      BpmnModel model, String currentStateId, NegotiationEvent event) {
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

  private Set<String> securityRoles(SequenceFlow flow) {
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
}
