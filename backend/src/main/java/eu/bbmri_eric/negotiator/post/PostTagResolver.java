package eu.bbmri_eric.negotiator.post;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class PostTagResolver {

  private final Map<TagKey, List<PostTagDTO>> cache = new HashMap<>();

  public List<PostTagDTO> resolve(Post post) {
    Person author = post.getCreatedBy();
    Negotiation negotiation = post.getNegotiation();
    TagKey key = new TagKey(negotiation.getId(), author.getId());
    return cache.computeIfAbsent(key, ignored -> calculateTags(author, negotiation));
  }

  private List<PostTagDTO> calculateTags(Person author, Negotiation negotiation) {
    EnumSet<PostTag> tags = EnumSet.noneOf(PostTag.class);
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
    return tags.stream().map(PostTag::toDTO).toList();
  }

  private record TagKey(String negotiationId, Long authorId) {}
}
