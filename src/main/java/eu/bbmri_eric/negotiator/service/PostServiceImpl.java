package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Post;
import eu.bbmri_eric.negotiator.database.model.PostStatus;
import eu.bbmri_eric.negotiator.database.model.PostType;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.PostRepository;
import eu.bbmri_eric.negotiator.dto.post.PostCreateDTO;
import eu.bbmri_eric.negotiator.dto.post.PostDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.exceptions.WrongRequestException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class PostServiceImpl implements PostService {
  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private PostRepository postRepository;

  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private PersonRepository personRepository;

  @Autowired private ModelMapper modelMapper;

  @Autowired private PersonService personService;
  @Autowired private NegotiationService negotiationService;
  @Autowired private UserNotificationService userNotificationService;

  @Transactional
  public PostDTO create(PostCreateDTO postRequest, Long personId, String negotiationId) {
    Post postEntity = setUpPostEntity(postRequest, negotiationId);
    try {
      postEntity = postRepository.save(postEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
    userNotificationService.notifyUsersAboutNewPost(postEntity);
    return modelMapper.map(postEntity, PostDTO.class);
  }

  @NonNull
  private Post setUpPostEntity(PostCreateDTO postRequest, String negotiationId) {
    Post postEntity = getPostEntity(postRequest);
    postEntity.setOrganization(getOrganization(postRequest));
    postEntity.setNegotiation(getNegotiation(negotiationId));
    Person author =
        personRepository
            .findById(NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())
            .orElseThrow(() -> new EntityNotFoundException("User with not found."));
    postEntity.setCreatedBy(author);
    postEntity.setStatus(PostStatus.CREATED);
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
  public List<PostDTO> findByNegotiationId(
      String negotiationId, @Nullable PostType type, @Nullable String organizationId) {
    List<Post> posts;
    if (type == null && organizationId == null) {
      posts = postRepository.findByNegotiationId(negotiationId);
    } else if (organizationId == null || organizationId.isEmpty()) {
      posts = postRepository.findByNegotiationIdAndType(negotiationId, type);
    } else if (type == null) {
      posts =
          postRepository.findByNegotiationIdAndOrganizationId(
              negotiationId, Long.valueOf(organizationId));
    } else {
      posts =
          postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
              negotiationId, type, organizationId);
    }
    return posts.stream()
        .filter(this::isAuthorized)
        .map(post -> modelMapper.map(post, PostDTO.class))
        .collect(Collectors.toList());
  }

  @Transactional
  public List<PostDTO> findNewByNegotiationIdAndAuthors(
      String negotiationId,
      List<String> authors,
      @Nullable PostType type,
      @Nullable String organizationId) {
    List<Post> posts;
    if (type == null && organizationId == null) {
      posts =
          postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameIn(
              negotiationId, PostStatus.CREATED, authors);
    } else if (organizationId == null || organizationId.isEmpty()) {
      posts =
          postRepository.findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameIn(
              negotiationId, PostStatus.CREATED, type, authors);
    } else if (type == null) {
      posts =
          postRepository.findByNegotiationIdAndStatusAndCreatedBy_NameInAndOrganization_ExternalId(
              negotiationId, PostStatus.CREATED, authors, organizationId);
    } else {
      posts =
          postRepository
              .findByNegotiationIdAndStatusAndTypeAndCreatedBy_NameInAndOrganization_ExternalId(
                  negotiationId, PostStatus.CREATED, type, authors, organizationId);
    }

    return posts.stream()
        .filter(this::isAuthorized)
        .map(post -> modelMapper.map(post, PostDTO.class))
        .collect(Collectors.toList());
  }

  @Transactional
  public PostDTO update(PostCreateDTO request, String negotiationId, String messageId) {
    Post post = postRepository.findByIdAndNegotiationId(messageId, negotiationId);
    post.setStatus(request.getStatus());
    post.setText(request.getText());
    Post updatedPost = postRepository.save(post);
    return modelMapper.map(updatedPost, PostDTO.class);
  }

  private boolean isRepresentative(Organization organization) {
    return personService.isRepresentativeOfAnyResourceOfOrganization(
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
        organization.getId());
  }

  private boolean isAdmin() {
    return NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin();
  }

  private boolean isAuthorized(Post post) {
    Negotiation negotiation = post.getNegotiation();
    if (isAdmin() || NegotiationServiceImpl.isNegotiationCreator(negotiation)) return true;
    return negotiationService.isAuthorizedForNegotiation(negotiation.getId())
        && (post.isPublic()
            || post.isCreator(
                NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())
            || (post.getOrganization() != null && isRepresentative(post.getOrganization())));
  }
}
