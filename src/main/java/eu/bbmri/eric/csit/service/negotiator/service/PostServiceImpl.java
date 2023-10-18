package eu.bbmri.eric.csit.service.negotiator.service;

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
  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired private PostRepository postRepository;

//  @Autowired private ResourceRepository resourceRepository;

  @Autowired private NegotiationRepository negotiationRepository;

  @Autowired private PersonRepository personRepository;

  @Autowired private ModelMapper modelMapper;

  @Transactional
  public PostDTO create(PostCreateDTO postRequest, Long personId, String negotiationId) {
    Post postEntity = modelMapper.map(postRequest, Post.class);

    if (postRequest.getType().equals(PostType.PRIVATE)) {
      // A private post is always associated and related to a Resource
      if (postRequest.getOrganizationId() != null) {
        String resourceId = postRequest.getOrganizationId();
        Organization organization =
            organizationRepository.findByExternalId(resourceId).orElseThrow(WrongRequestException::new);
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
    if (type == null) {
      posts = postRepository.findByNegotiationId(negotiationId);
    } else if (organizationId == null || organizationId.isEmpty()) {
      posts = postRepository.findByNegotiationIdAndType(negotiationId, type);
    } else {
      posts = postRepository.findByNegotiationIdAndTypeAndOrganization(negotiationId, type, organizationId);
    }
    return posts.stream()
        .map(post -> modelMapper.map(post, PostDTO.class))
        .collect(Collectors.toList());
  }

  @Transactional
  public List<PostDTO> findNewByNegotiationIdAndPosters(
      String negotiationId,
      List<String> posters,
      @Nullable PostType type,
      @Nullable String organizationId) {
    List<Post> posts;
    if (type == null) {
      posts = postRepository.findNewByNegotiationIdAndPosters(negotiationId, posters);
    } else if (organizationId == null || organizationId.isEmpty()) {
      posts = postRepository.findNewByNegotiationIdAndPostersAndType(negotiationId, posters, type);
    } else {
      posts =
          postRepository.findNewByNegotiationIdAndPostersAndTypeAndOrganizationId(
              negotiationId, posters, type, organizationId);
    }
    return posts.stream()
        .map(post -> modelMapper.map(post, PostDTO.class))
        .collect(Collectors.toList());
  }

  @Transactional
  public PostDTO update(PostCreateDTO request, String negotiationId, String messageId) {
    Post post = postRepository.findByNegotiationIdAndId(negotiationId, messageId);
    post.setStatus(request.getStatus());
    post.setText(request.getText());
    Post updatedPost = postRepository.save(post);
    return modelMapper.map(updatedPost, PostDTO.class);
  }
}
