package eu.bbmri_eric.negotiator.post;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationAccessManager;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.notification.OldNotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.PersonService;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class PostServiceImpl implements PostService {
  private OrganizationRepository organizationRepository;

  private PostRepository postRepository;

  private NegotiationRepository negotiationRepository;
  private PersonRepository personRepository;

  private ModelMapper modelMapper;

  private PersonService personService;
  private NegotiationService negotiationService;
  private OldNotificationService oldNotificationService;
  private NegotiationAccessManager negotiationAccessManager;
  private final ApplicationEventPublisher applicationEventPublisher;

  public PostServiceImpl(
          OrganizationRepository organizationRepository,
          PostRepository postRepository,
          NegotiationRepository negotiationRepository,
          PersonRepository personRepository,
          ModelMapper modelMapper,
          PersonService personService,
          NegotiationService negotiationService,
          OldNotificationService oldNotificationService,
          NegotiationAccessManager negotiationAccessManager, ApplicationEventPublisher applicationEventPublisher) {
    this.organizationRepository = organizationRepository;
    this.postRepository = postRepository;
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
    this.modelMapper = modelMapper;
    this.personService = personService;
    this.negotiationService = negotiationService;
    this.oldNotificationService = oldNotificationService;
    this.negotiationAccessManager = negotiationAccessManager;
      this.applicationEventPublisher = applicationEventPublisher;
  }

  private static Post getPostEntity(PostCreateDTO postRequest) {
    return Post.builder().text(postRequest.getText()).type(postRequest.getType()).build();
  }

  /**
   * Checks whether the post is allowed.
   *
   * @return true if the post's type is enabled for the Negotiation
   */
  private boolean isPostAllowed(PostCreateDTO postDTO, Negotiation negotiation) {
    return (postDTO.getType() == PostType.PRIVATE && negotiation.isPrivatePostsEnabled())
        || (postDTO.getType() == PostType.PUBLIC && negotiation.isPublicPostsEnabled());
  }

  @Transactional
  public PostDTO create(PostCreateDTO postRequest, String negotiationId) {
    Negotiation negotiation = getNegotiation(negotiationId);
    checkAuthorization(postRequest, negotiationId, negotiation);
    Post postEntity = setUpPostEntity(postRequest, negotiation);
    try {
      postEntity = postRepository.save(postEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
    applicationEventPublisher.publishEvent(new NewPostEvent(this, postEntity.getId(), negotiationId));
    return modelMapper.map(postEntity, PostDTO.class);
  }

  private void checkAuthorization(
      PostCreateDTO postRequest, String negotiationId, Negotiation negotiation) {
    if (!negotiationService.isAuthorizedForNegotiation(negotiationId)
        && !AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()
        && !personRepository.isManagerOfAnyResourceOfNegotiation(
            AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), negotiationId)) {
      throw new ForbiddenRequestException(
          "You're not authorized to send messages to this negotiation");
    }
    if (!isPostAllowed(postRequest, negotiation)) {
      throw new ForbiddenRequestException(
          "%s posts are not currently allowed for this negotiation"
              .formatted(postRequest.getType()));
    }
  }

  @Override
  public PostDTO findById(String id) {
    return null;
  }

  @NonNull
  private Post setUpPostEntity(PostCreateDTO postRequest, Negotiation negotiation) {
    Post postEntity = getPostEntity(postRequest);
    postEntity.setOrganization(getOrganization(postRequest));
    postEntity.setNegotiation(negotiation);
    Long authorId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    Person author =
        personRepository
            .findById(authorId)
            .orElseThrow(() -> new EntityNotFoundException(authorId));
    postEntity.setCreatedBy(author);
    return postEntity;
  }

  private Negotiation getNegotiation(String negotiationId) {
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private Organization getOrganization(PostCreateDTO postRequest) {
    if (postRequest.getType().equals(PostType.PRIVATE)) {
      if (postRequest.getOrganizationId() != null) {
        return organizationRepository
            .findByExternalId(postRequest.getOrganizationId())
            .orElseThrow(WrongRequestException::new);
      }
    }
    return null;
  }

  @Transactional
  public List<PostDTO> findByNegotiationId(String negotiationId) {
    if (!negotiationRepository.existsById(negotiationId)) {
      throw new EntityNotFoundException(negotiationId);
    }
    verifyReadAccess(negotiationId);
    List<Post> allNegotiationPosts = postRepository.findByNegotiationId(negotiationId);
    List<Post> readablePosts = getReadablePosts(allNegotiationPosts);
    if (isNegotiationCreatorOrAdmin(negotiationId)) {
      readablePosts.addAll(getAllUnreadablePosts(allNegotiationPosts));
    } else {
      addUserAccessiblePosts(allNegotiationPosts, readablePosts);
    }
    return sortedPosts(readablePosts);
  }

  private @NotNull List<PostDTO> sortedPosts(List<Post> readablePosts) {
    try {
      readablePosts.sort(Comparator.comparing(Post::getCreationDate));
    } catch (NullPointerException e) {
      log.warn("Error sorting posts");
    }
    return readablePosts.stream()
        .map(post -> modelMapper.map(post, PostDTO.class))
        .collect(Collectors.toList());
  }

  private void verifyReadAccess(String negotiationId) {
    negotiationAccessManager.verifyReadAccessForNegotiation(
        negotiationId, AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId());
  }

  private List<Post> getReadablePosts(List<Post> allNegotiationPosts) {
    return allNegotiationPosts.stream().filter(Post::isPublic).collect(Collectors.toList());
  }

  private boolean isNegotiationCreatorOrAdmin(String negotiationId) {
    return negotiationService.isNegotiationCreator(negotiationId) || isAdmin();
  }

  private List<Post> getAllUnreadablePosts(List<Post> allNegotiationPosts) {
    return allNegotiationPosts.stream()
        .filter(post -> !post.isPublic())
        .collect(Collectors.toList());
  }

  private void addUserAccessiblePosts(List<Post> allNegotiationPosts, List<Post> readablePosts) {
    Person user = getCurrentUser();
    Set<Organization> accessibleOrganizations = getUserAccessibleOrganizations(user);

    allNegotiationPosts.stream()
        .filter(
            post -> !post.isPublic() && accessibleOrganizations.contains(post.getOrganization()))
        .forEach(readablePosts::add);
  }

  private Person getCurrentUser() {
    Long authorId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    return personRepository
        .findById(authorId)
        .orElseThrow(() -> new EntityNotFoundException(authorId));
  }

  private Set<Organization> getUserAccessibleOrganizations(Person user) {
    Set<Organization> organizations =
        user.getResources().stream().map(Resource::getOrganization).collect(Collectors.toSet());

    organizations.addAll(
        user.getNetworks().stream()
            .flatMap(network -> network.getResources().stream())
            .map(Resource::getOrganization)
            .collect(Collectors.toSet()));

    return organizations;
  }

  private boolean isAdmin() {
    return AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin();
  }

  @Transactional
  public PostDTO update(PostCreateDTO request, String negotiationId, String messageId) {
    Post post = postRepository.findByIdAndNegotiationId(messageId, negotiationId);
    post.setText(request.getText());
    Post updatedPost = postRepository.save(post);
    return modelMapper.map(updatedPost, PostDTO.class);
  }
}
