package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Post;
import eu.bbmri_eric.negotiator.database.model.PostStatus;
import eu.bbmri_eric.negotiator.database.model.PostType;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.PostRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.post.PostCreateDTO;
import eu.bbmri_eric.negotiator.dto.post.PostDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.exceptions.WrongRequestException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommonsLog
public class PostServiceImpl implements PostService {

  @Autowired private PostRepository postRepository;

  @Autowired private ResourceRepository resourceRepository;

  @Autowired private NegotiationRepository negotiationRepository;

  @Autowired private PersonRepository personRepository;

  @Autowired private ModelMapper modelMapper;

  @Transactional
  public PostDTO create(PostCreateDTO postRequest, Long personId, String negotiationId) {
    Post postEntity = modelMapper.map(postRequest, Post.class);
    try {
      Resource resource = null;
      if (postRequest.getType().equals(PostType.PRIVATE)) {
        // A private post is always associated and related to a Resource
        String resourceId = postRequest.getResourceId();
        resource =
            resourceRepository.findBySourceId(resourceId).orElseThrow(WrongRequestException::new);
      }
      Negotiation negotiation =
          negotiationRepository
              .findById(negotiationId)
              .orElseThrow(() -> new EntityNotFoundException(negotiationId));

      Person person =
          personRepository.findDetailedById(personId).orElseThrow(WrongRequestException::new);

      postEntity.setResource(resource);
      postEntity.setNegotiation(negotiation);
      postEntity.setStatus(PostStatus.CREATED);

      Post post = postRepository.save(postEntity);
      return modelMapper.map(post, PostDTO.class);

    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  @Transactional
  public List<PostDTO> findByNegotiationId(
      String negotiationId, @Nullable PostType type, @Nullable String resourceId) {
    List<Post> posts;
    if (type == null) {
      posts = postRepository.findByNegotiationId(negotiationId);
    } else if (resourceId == null || resourceId.isEmpty()) {
      posts = postRepository.findByNegotiationIdAndType(negotiationId, type);
    } else {
      posts = postRepository.findByNegotiationIdAndTypeAndResource(negotiationId, type, resourceId);
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
      @Nullable String resourceId) {
    List<Post> posts;
    if (type == null) {
      posts = postRepository.findNewByNegotiationIdAndPosters(negotiationId, posters);
    } else if (resourceId == null || resourceId.isEmpty()) {
      posts = postRepository.findNewByNegotiationIdAndPostersAndType(negotiationId, posters, type);
    } else {
      posts =
          postRepository.findNewByNegotiationIdAndPostersAndTypeAndResource(
              negotiationId, posters, type, resourceId);
    }
    return posts.stream()
        .map(post -> modelMapper.map(post, PostDTO.class))
        .collect(Collectors.toList());
  }

  @Transactional
  public PostDTO update(PostCreateDTO request, String negotiationId, String messageId) {
    Post post = postRepository.findByNegotiationIdAndMessageId(negotiationId, messageId);
    post.setStatus(request.getStatus());
    post.setText(request.getText());
    Post updatedPost = postRepository.save(post);
    return modelMapper.map(updatedPost, PostDTO.class);
  }
}
