package eu.bbmri_eric.negotiator.negotiation;

import com.vladmihalcea.hibernate.type.json.JsonType;
import eu.bbmri_eric.negotiator.attachment.Attachment;
import eu.bbmri_eric.negotiator.common.AuditEntity;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
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
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** A Core Entity representing a Negotiation between multiple parties about access to resources. */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Convert(converter = JsonType.class, attributeName = "json")
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

  @Column(columnDefinition = "TEXT")
  private String humanReadable = "";

  @OneToMany(
      mappedBy = "id.negotiation",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE})
  @NotNull
  @Builder.Default
  private Set<NegotiationResourceLink> resourcesLink = new HashSet<>();

  @Formula(value = "JSONB_EXTRACT_PATH_TEXT(payload, 'project', 'title')")
  private String title;

  @JdbcTypeCode(SqlTypes.JSON)
  private String payload;

  private boolean publicPostsEnabled = true;

  private boolean privatePostsEnabled = false;

  @Builder.Default
  @Setter(AccessLevel.NONE)
  @Enumerated(EnumType.STRING)
  private NegotiationState currentState = NegotiationState.SUBMITTED;

  public void setAttachments(Set<Attachment> attachments) {
    if (attachments != null) {
      attachments.forEach(attachment -> attachment.setNegotiation(this));
      this.attachments = attachments;
    }
  }

  @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE})
  @JoinColumn(name = "negotiation_id", referencedColumnName = "id")
  @Setter(AccessLevel.NONE)
  @Builder.Default
  private Set<NegotiationLifecycleRecord> lifecycleHistory = creteInitialHistory();

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

  public void setCurrentState(NegotiationState negotiationState) {
    this.currentState = negotiationState;
    this.lifecycleHistory.add(NegotiationLifecycleRecord.builder().changedTo(currentState).build());
  }

  public NegotiationResourceState getCurrentStateForResource(String resourceId) {
    return this.resourcesLink.stream()
        .filter(link -> link.getResource().getSourceId().equals(resourceId))
        .findFirst()
        .orElseThrow(IllegalArgumentException::new)
        .getCurrentState();
  }

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
   * Link a resource to the Negotiation with null state.
   *
   * @param resource to be linked.
   */
  public void addResource(Resource resource) {
    this.resourcesLink.add(new NegotiationResourceLink(this, resource, null));
  }

  private void buildResourceStateChangeRecord(Resource resource, NegotiationResourceState state) {
    if (!state.equals(NegotiationResourceState.SUBMITTED)) {
      NegotiationResourceLifecycleRecord record =
          NegotiationResourceLifecycleRecord.builder().changedTo(state).resource(resource).build();
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

  public static class CustomNegotiationBuilder extends NegotiationBuilder {
    private Set<Resource> resources;

    @Override
    public CustomNegotiationBuilder resources(Set<Resource> resources) {
      this.resources = resources;
      return this;
    }

    @Override
    public Negotiation build() {
      Negotiation negotiation = super.build();
      if (this.resources != null) {
        negotiation.setResources(this.resources);
      }
      return negotiation;
    }
  }
}
