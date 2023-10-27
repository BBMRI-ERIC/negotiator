package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetailsService;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.database.repository.*;
import eu.bbmri.eric.csit.service.negotiator.dto.post.PostCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.post.PostDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
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

  @Autowired private ModelMapper modelMapper;

  @Transactional
  public PostDTO create(PostCreateDTO postRequest, Long personId, String negotiationId) {
    Post postEntity = modelMapper.map(postRequest, Post.class);

    if (postRequest.getType().equals(PostType.PRIVATE)) {
      // A private post is always associated and related to a Resource
      if (postRequest.getOrganizationId() != null) {
        String resourceId = postRequest.getOrganizationId();
        Organization organization =
            organizationRepository
                .findByExternalId(resourceId)
                .orElseThrow(WrongRequestException::new);
        postEntity.setOrganization(organization);
      }
    }

    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));

    postEntity.setNegotiation(negotiation);
    postEntity.setStatus(PostStatus.CREATED);

    try {
      Post post = postRepository.save(postEntity);
      return modelMapper.map(post, PostDTO.class);

    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
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
      posts = postRepository.findByNegotiationIdAndOrganizationId(negotiationId, organizationId);
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
          postRepository.findByNegotiationIdAndStatusAndCreatedBy_authNameIn(
              negotiationId, PostStatus.CREATED, authors);
    } else if (organizationId == null || organizationId.isEmpty()) {
      posts =
          postRepository.findByNegotiationIdAndStatusAndTypeAndCreatedBy_authNameIn(
              negotiationId, PostStatus.CREATED, type, authors);
    } else if (type == null) {
      posts =
          postRepository
              .findByNegotiationIdAndStatusAndCreatedBy_authNameInAndOrganization_ExternalId(
                  negotiationId, PostStatus.CREATED, authors, organizationId);
    } else {
      posts =
          postRepository
              .findByNegotiationIdAndStatusAndTypeAndCreatedBy_authNameInAndOrganization_ExternalId(
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
    return NegotiatorUserDetailsService.isRepresentativeAny(
        organization.getResources().stream().map(Resource::getSourceId).toList());
  }

  private boolean isAdmin() {
    return NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin();
  }

  private boolean isAuthorized(Post post) {
    Negotiation negotiation = post.getNegotiation();
    // admin and negotiation creator see all posts
    if (isAdmin() || NegotiationServiceImpl.isNegotiationCreator(negotiation)) return true;
    // For the user to access the post, he/she has to be authorized for the negotiation and the post
    // must be either:
    // 1. public (in the negotiation)
    // 2. created by the authenticated user
    // 3. addressed to the organization represented by the authenticated user
    boolean negoauth = NegotiationServiceImpl.isAuthorizedForNegotiation(negotiation);
    return NegotiationServiceImpl.isAuthorizedForNegotiation(negotiation)
        && (post.isPublic() // The post is public so everyone in the negotiation can see it
            || post.isCreator(
                NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())
            || (post.getOrganization() != null && isRepresentative(post.getOrganization())));
  }
}
