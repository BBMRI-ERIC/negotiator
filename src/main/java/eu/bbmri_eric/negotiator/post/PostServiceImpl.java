package eu.bbmri_eric.negotiator.post;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.notification.UserNotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.PersonService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
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
  private UserNotificationService userNotificationService;
  private AuthenticatedUserContext userDetailsService;

  public PostServiceImpl(
      OrganizationRepository organizationRepository,
      PostRepository postRepository,
      NegotiationRepository negotiationRepository,
      PersonRepository personRepository,
      ModelMapper modelMapper,
      PersonService personService,
      NegotiationService negotiationService,
      UserNotificationService userNotificationService,
      AuthenticatedUserContext userDetailsService) {
    this.organizationRepository = organizationRepository;
    this.postRepository = postRepository;
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
    this.modelMapper = modelMapper;
    this.personService = personService;
    this.negotiationService = negotiationService;
    this.userNotificationService = userNotificationService;
    this.userDetailsService = userDetailsService;
  }

  /**
   * Checks whether the post is allowed. The choice is made according to the postsEnabled flags of
   * the Negotiation and the post type
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
    userNotificationService.notifyUsersAboutNewPost(postEntity);
    return modelMapper.map(postEntity, PostDTO.class);
  }

  private void checkAuthorization(
      PostCreateDTO postRequest, String negotiationId, Negotiation negotiation) {
    if (!negotiationService.isAuthorizedForNegotiation(negotiationId)
        && !AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
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
    Person author =
        personRepository
            .findById(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())
            .orElseThrow(() -> new EntityNotFoundException("User with not found."));
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

  private static Post getPostEntity(PostCreateDTO postRequest) {
    return Post.builder().text(postRequest.getText()).type(postRequest.getType()).build();
  }

  @Transactional
  public List<PostDTO> findByNegotiationId(String negotiationId) {
    List<Post> posts = postRepository.findByNegotiationId(negotiationId);
    return posts.stream()
        .filter(this::isAuthorized)
        .map(post -> modelMapper.map(post, PostDTO.class))
        .toList();
  }

  @Transactional
  public PostDTO update(PostCreateDTO request, String negotiationId, String messageId) {
    Post post = postRepository.findByIdAndNegotiationId(messageId, negotiationId);
    post.setText(request.getText());
    Post updatedPost = postRepository.save(post);
    return modelMapper.map(updatedPost, PostDTO.class);
  }

  private boolean isRepresentative(Organization organization) {
    return personService.isRepresentativeOfAnyResourceOfOrganization(
        AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), organization.getId());
  }

  private boolean isAdmin() {
    return AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin();
  }

  private boolean isAuthorized(Post post) {
    Negotiation negotiation = post.getNegotiation();
    if (isAdmin() || negotiationService.isNegotiationCreator(negotiation.getId())) return true;
    return negotiationService.isAuthorizedForNegotiation(negotiation.getId())
        && (post.isPublic()
            || post.isCreator(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())
            || (post.getOrganization() != null && isRepresentative(post.getOrganization())));
  }
}
