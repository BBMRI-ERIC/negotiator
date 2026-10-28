package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostTagDTO;
import eu.bbmri_eric.negotiator.post.PostTagResolver;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostTagResolverTest {

  private static final String NEG_1 = "negotiation-1";

  private PostTagResolver resolver;
  private Negotiation negotiation;
  private Organization organization1;
  private Person researcher;
  private Person biobanker1;
  private Person admin;
  private Person networkManager;
  private Person plainUser;

  @BeforeEach
  void before() {
    resolver = new PostTagResolver();

    researcher = Person.builder().id(1L).name("researcher").build();
    biobanker1 = Person.builder().id(2L).name("biobanker1").build();
    admin = Person.builder().id(3L).name("admin").admin(true).build();
    plainUser = Person.builder().id(4L).name("plain").build();
    networkManager = Person.builder().id(5L).name("networkManager").build();
    Network network = Network.builder().id(1L).externalId("network:1").name("Network 1").build();
    network.addManager(networkManager);

    organization1 = Organization.builder().id(1L).externalId("org:1").build();
    Resource resource1 =
        Resource.builder()
            .sourceId("resource:1")
            .name("Resource 1")
            .organization(organization1)
            .build();
    organization1.setResources(Set.of(resource1));

    negotiation = Negotiation.builder().id(NEG_1).build();
    negotiation.setCreatedBy(researcher);
  }

  @Test
  void resolve_negotiationCreator_getsAuthorTag() {
    List<PostTagDTO> tags = resolver.resolve(post(researcher));

    assertTrue(hasTag(tags, "author"));
  }

  @Test
  void resolve_adminNegotiationCreator_getsOnlyAuthorTag() {
    researcher.setAdmin(true);

    List<PostTagDTO> tags = resolver.resolve(post(researcher));

    assertEquals(List.of("author"), tags.stream().map(PostTagDTO::key).toList());
  }

  @Test
  void resolve_adminNegotiationCreatorWithOtherRoles_keepsOtherTagsWithoutAdminTag() {
    networkManager.setAdmin(true);
    networkManager.setResources(Set.of(organization1.getResources().iterator().next()));
    negotiation.setCreatedBy(networkManager);

    List<PostTagDTO> tags = resolver.resolve(post(networkManager));

    assertEquals(
        List.of("author", "representative", "network-manager"),
        tags.stream().map(PostTagDTO::key).toList());
  }

  @Test
  void resolve_resourceRepresentative_getsRepresentativeTag() {
    biobanker1.setResources(Set.of(organization1.getResources().iterator().next()));

    List<PostTagDTO> tags = resolver.resolve(post(biobanker1));

    assertTrue(hasTag(tags, "representative"));
    assertFalse(hasTag(tags, "author"));
  }

  @Test
  void resolve_admin_getsAdminTag() {
    List<PostTagDTO> tags = resolver.resolve(post(admin));

    assertTrue(hasTag(tags, "admin"));
  }

  @Test
  void resolve_adminWithoutNegotiationCreator_getsOnlyAdminTag() {
    negotiation.setCreatedBy(null);

    List<PostTagDTO> tags = resolver.resolve(post(admin));

    assertEquals(List.of("admin"), tags.stream().map(PostTagDTO::key).toList());
  }

  @Test
  void resolve_networkManager_getsNetworkManagerTag() {
    List<PostTagDTO> tags = resolver.resolve(post(networkManager));

    assertTrue(hasTag(tags, "network-manager"));
  }

  @Test
  void resolve_plainUser_getsNoTags() {
    List<PostTagDTO> tags = resolver.resolve(post(plainUser));

    assertTrue(tags.isEmpty());
  }

  @Test
  void resolve_networkManagerAndAdmin_getsBothTags() {
    networkManager.setAdmin(true);

    List<PostTagDTO> tags = resolver.resolve(post(networkManager));

    assertTrue(hasTag(tags, "network-manager"));
    assertTrue(hasTag(tags, "admin"));
  }

  @Test
  void resolve_sameAuthorInSameNegotiation_returnsCachedInstance() {
    List<PostTagDTO> first = resolver.resolve(post(researcher));
    researcher.setAdmin(true);
    List<PostTagDTO> second = resolver.resolve(post(researcher));

    assertEquals(first, second);
    assertSame(
        first, second, "tags for the same (negotiation, author) should be served from cache");
  }

  private Post post(Person author) {
    Post post = Post.builder().negotiation(negotiation).text("text").type(PostType.PUBLIC).build();
    post.setCreatedBy(author);
    return post;
  }

  private static boolean hasTag(List<PostTagDTO> tags, String key) {
    return tags.stream().anyMatch(tag -> tag.key().equals(key));
  }
}
