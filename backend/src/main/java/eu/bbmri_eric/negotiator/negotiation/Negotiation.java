package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.attachment.Attachment;
import eu.bbmri_eric.negotiator.common.AuditEntity;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.post.Post;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;

/** A Core Entity representing a Negotiation between multiple parties about access to resources. */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Convert(converter = JsonType.class, attributeName = "json")
@ToString
public class Negotiation extends AuditEntity {

  @Id
  @GeneratedValue(generator = "uuid")
  @UuidGenerator
  @Column(name = "id")
  private String id;

  @OneToMany(
      mappedBy = "negotiation",
      cascade = {CascadeType.MERGE})
  @Builder.Default
  private Set<Attachment> attachments = new HashSet<>();

  @OneToMany(
      mappedBy = "negotiation",
      cascade = {CascadeType.REMOVE})
  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.PUBLIC)
  @Builder.Default
  private Set<Post> posts = new HashSet<>();

  @Column(columnDefinition = "TEXT")
  private String humanReadable = "";

  @OneToMany(
      mappedBy = "id.negotiation",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE})
  @NotNull
  @Builder.Default
  @Getter(AccessLevel.PRIVATE)
  @Setter(AccessLevel.PRIVATE)
  private Set<NegotiationResourceLink> resourcesLink = new HashSet<>();

  @Formula(value = "JSON_EXTRACT_PATH_TEXT(payload, 'project', 'title')")
  private String title;

  @Type(JsonType.class)
  @Column(columnDefinition = "json")
  @ColumnTransformer(read = "payload::text", write = "?::json")
  private String payload;

  private boolean publicPostsEnabled;

  private boolean privatePostsEnabled = false;

  @Setter(AccessLevel.NONE)
  @Enumerated(EnumType.STRING)
  private NegotiationState currentState;

  @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE})
  @JoinColumn(name = "negotiation_id", referencedColumnName = "id")
  @Setter(AccessLevel.NONE)
  @Builder.Default
  private Set<NegotiationLifecycleRecord> lifecycleHistory = new HashSet<>();

  @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE})
  @JoinColumn(name = "negotiation_id", referencedColumnName = "id")
  @Setter(AccessLevel.NONE)
  @Builder.Default
  private Set<NegotiationResourceLifecycleRecord> negotiationResourceLifecycleRecords =
      new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "discovery_service_id")
  @NotNull
  private DiscoveryService discoveryService;

  public void setAttachments(Set<Attachment> attachments) {
    if (attachments != null) {
      attachments.forEach(attachment -> attachment.setNegotiation(this));
      this.attachments = attachments;
    }
  }

  public void setCurrentState(NegotiationState negotiationState) {
    this.currentState = negotiationState;
    this.lifecycleHistory.add(NegotiationLifecycleRecord.builder().changedTo(currentState).build());
  }

  /**
   * Gets the current state for a liked Resource.
   *
   * @param resourceId the source/external ID of the Resource. Not the internal ID!
   */
  public NegotiationResourceState getCurrentStateForResource(String resourceId) {
    return this.resourcesLink.stream()
        .filter(link -> link.getResource().getSourceId().equals(resourceId))
        .findFirst()
        .orElseThrow(IllegalArgumentException::new)
        .getCurrentState();
  }

  /**
   * Sets the current state for a liked Resource.
   *
   * @param resourceId the source/external ID of the Resource. Not the internal ID!
   * @param state to be set.
   */
  public void setStateForResource(String resourceId, NegotiationResourceState state) {
    NegotiationResourceLink link =
        this.resourcesLink.stream()
            .filter(resourceLink -> resourceLink.getResource().getSourceId().equals(resourceId))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    link.setCurrentState(state);
    buildResourceStateChangeRecord(link.getResource(), state);
  }

  public Set<Resource> getResources() {
    return getResourcesLink().stream()
        .map(NegotiationResourceLink::getResource)
        .collect(Collectors.toSet());
  }

  /**
   * Get all organizations involved in the Negotiation.
   *
   * @return a set of involved Organizations
   */
  public Set<Organization> getOrganizations() {
    return getResourcesLink().stream()
        .map(NegotiationResourceLink::getResource)
        .collect(Collectors.toSet())
        .stream()
        .map(Resource::getOrganization)
        .collect(Collectors.toSet());
  }

  /**
   * Set all the resources linked to the Negotiation. This operation will also clear their
   * state.Consider using {@link
   * #addResource(eu.bbmri_eric.negotiator.governance.resource.Resource)}
   *
   * @param resources to be linked.
   */
  public void setResources(Set<Resource> resources) {
    this.resourcesLink.clear();
    resources.forEach(this::addResource);
  }

  /**
   * Link a resource to the Negotiation with null state. If resource is already linked, the
   * Resources remain unchanged.
   *
   * @param resource to be linked.
   */
  public boolean addResource(Resource resource) {
    return this.resourcesLink.add(new NegotiationResourceLink(this, resource, null));
  }

  private void buildResourceStateChangeRecord(Resource resource, NegotiationResourceState state) {
    if (!state.equals(NegotiationResourceState.SUBMITTED)) {
      NegotiationResourceLifecycleRecord record =
          NegotiationResourceLifecycleRecord.builder().changedTo(state).resource(resource).build();
      record.setCreationDate(LocalDateTime.now());
      record.setModifiedDate(LocalDateTime.now());
      this.negotiationResourceLifecycleRecords.add(record);
    }
  }

  private static Set<NegotiationLifecycleRecord> creteInitialHistory() {
    Set<NegotiationLifecycleRecord> history = new HashSet<>();
    history.add(NegotiationLifecycleRecord.builder().changedTo(NegotiationState.SUBMITTED).build());
    return history;
  }

  public static CustomNegotiationBuilder builder() {
    return new CustomNegotiationBuilder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Negotiation negotiation = (Negotiation) o;
    return Objects.equals(getId(), negotiation.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  public static class NegotiationBuilder {
    public NegotiationBuilder resources(Set<Resource> resources) {
      return this;
    }
  }
}
