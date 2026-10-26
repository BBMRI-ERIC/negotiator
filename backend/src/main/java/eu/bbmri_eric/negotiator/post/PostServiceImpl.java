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
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
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

  private NegotiationService negotiationService;
  private NegotiationAccessManager negotiationAccessManager;
  private final ApplicationEventPublisher applicationEventPublisher;

  public PostServiceImpl(
      OrganizationRepository organizationRepository,
      PostRepository postRepository,
      NegotiationRepository negotiationRepository,
      PersonRepository personRepository,
      ModelMapper modelMapper,
      NegotiationService negotiationService,
      NegotiationAccessManager negotiationAccessManager,
      ApplicationEventPublisher applicationEventPublisher) {
    this.organizationRepository = organizationRepository;
    this.postRepository = postRepository;
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
    this.modelMapper = modelMapper;
    this.negotiationService = negotiationService;
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
    applicationEventPublisher.publishEvent(
        new NewPostEvent(
            this,
            postEntity.getId(),
            negotiationId,
            postEntity.getCreatedBy().getId(),
            postEntity.getOrganization() != null ? postEntity.getOrganization().getId() : null));
    PostDTO postDTO = modelMapper.map(postEntity, PostDTO.class);
    postDTO.setTags(toDTOs(computeTags(postEntity.getCreatedBy(), negotiation)));
    return postDTO;
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
  @Transactional
  public PostDTO findById(String negotiationId, String id) {
    Negotiation negotiation = getNegotiation(negotiationId);
    Post post = postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    verifyReadAccess(negotiationId);
    if (!post.isPublic() && !negotiationService.isNegotiationEditor(negotiationId)) {
      Person user = getCurrentUser();
      boolean isRepresentativeOfPostOrganization =
          post.getOrganization().getResources().stream()
              .flatMap(resource -> resource.getRepresentatives().stream())
              .anyMatch(person -> person.getId().equals(user.getId()));
      if (!isRepresentativeOfPostOrganization) {
        throw new ForbiddenRequestException("You are not authorized to read this post");
      }
    }
    PostDTO postDTO = modelMapper.map(post, PostDTO.class);
    postDTO.setTags(toDTOs(computeTags(post.getCreatedBy(), negotiation)));
    return postDTO;
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
    Negotiation negotiation = getNegotiation(negotiationId);
    verifyReadAccess(negotiationId);
    List<Post> allNegotiationPosts = postRepository.findByNegotiationId(negotiationId);
    List<Post> readablePosts = getReadablePosts(allNegotiationPosts);
    if (negotiationService.isNegotiationEditor(negotiationId)) {
      readablePosts.addAll(getAllUnreadablePosts(allNegotiationPosts));
    } else {
      addUserAccessiblePosts(allNegotiationPosts, readablePosts);
    }
    return sortedPosts(readablePosts, negotiation);
  }

  private @NotNull List<PostDTO> sortedPosts(List<Post> readablePosts, Negotiation negotiation) {
    try {
      readablePosts.sort(Comparator.comparing(Post::getCreationDate));
    } catch (NullPointerException e) {
      log.warn("Error sorting posts");
    }
    Map<String, Set<PostTag>> tagsByUserId = new HashMap<>();
    return readablePosts.stream()
        .map(
            post -> {
              PostDTO dto = modelMapper.map(post, PostDTO.class);
              Set<PostTag> tags =
                  tagsByUserId.computeIfAbsent(
                      post.getCreatedBy().getId().toString(),
                      userId -> computeTags(post.getCreatedBy(), negotiation));
              dto.setTags(toDTOs(tags));
              return dto;
            })
        .collect(Collectors.toList());
  }

  private Set<PostTag> computeTags(Person author, Negotiation negotiation) {
    Set<PostTag> tags = EnumSet.noneOf(PostTag.class);
    if (negotiation.getCreatedBy() != null
        && author.getId().equals(negotiation.getCreatedBy().getId())) {
      tags.add(PostTag.AUTHOR);
    }
    if (author.getResources() != null && !author.getResources().isEmpty()) {
      tags.add(PostTag.REPRESENTATIVE);
    }
    if (author.isAdmin()) {
      tags.add(PostTag.ADMIN);
    }
    if (author.getNetworks() != null && !author.getNetworks().isEmpty()) {
      tags.add(PostTag.NETWORK_MANAGER);
    }
    return tags;
  }

  private List<PostTagDTO> toDTOs(Set<PostTag> tags) {
    return tags.stream().map(PostTag::toDTO).toList();
  }

  private void verifyReadAccess(String negotiationId) {
    negotiationAccessManager.verifyReadAccessForNegotiation(
        negotiationId, AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId());
  }

  private List<Post> getReadablePosts(List<Post> allNegotiationPosts) {
    return allNegotiationPosts.stream().filter(Post::isPublic).collect(Collectors.toList());
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

  @Transactional
  public PostDTO update(PostCreateDTO request, String negotiationId, String messageId) {
    Negotiation negotiation = getNegotiation(negotiationId);
    Post post = postRepository.findByIdAndNegotiationId(messageId, negotiationId);
    post.setText(request.getText());
    Post updatedPost = postRepository.save(post);
    PostDTO postDTO = modelMapper.map(updatedPost, PostDTO.class);
    postDTO.setTags(toDTOs(computeTags(updatedPost.getCreatedBy(), negotiation)));
    return postDTO;
  }
}
