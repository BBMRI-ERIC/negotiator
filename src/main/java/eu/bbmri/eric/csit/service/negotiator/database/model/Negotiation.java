package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString.Exclude;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "negotiation")
@Convert(converter = JsonType.class, attributeName = "json")
@NamedEntityGraph(
    name = "negotiation-with-detailed-children",
    attributeNodes = {
      @NamedAttributeNode(value = "persons", subgraph = "persons-with-roles"),
      @NamedAttributeNode(value = "requests", subgraph = "requests-detailed"),
      @NamedAttributeNode(value = "attachments"),
      @NamedAttributeNode(value = "currentStatePerResource")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "persons-with-roles",
          attributeNodes = {
            @NamedAttributeNode(value = "person"),
            @NamedAttributeNode(value = "role")
          }),
      @NamedSubgraph(
          name = "requests-detailed",
          attributeNodes = {@NamedAttributeNode(value = "resources")})
    })
public class Negotiation extends AuditEntity {

  @Id
  @GeneratedValue(generator = "uuid")
  @UuidGenerator
  @Column(name = "id")
  private String id;

  @OneToMany(
      mappedBy = "negotiation",
      cascade = {CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private Set<Attachment> attachments;

  @OneToMany(
      mappedBy = "negotiation",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
      fetch = FetchType.LAZY)
  @Exclude
  private Set<PersonNegotiationRole> persons = new HashSet<>();

  @OneToMany(mappedBy = "negotiation", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  @Exclude
  private Set<Request> requests;

  @JdbcTypeCode(SqlTypes.JSON)
  private String payload;

  private Boolean postsEnabled = false;

  @Builder.Default
  @Setter(AccessLevel.NONE)
  @Enumerated(EnumType.STRING)
  private NegotiationState currentState = NegotiationState.SUBMITTED;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "resource_state_per_negotiation",
      joinColumns = {@JoinColumn(name = "negotiation_id", referencedColumnName = "id")})
  @MapKeyColumn(name = "resource_id")
  @Enumerated(EnumType.STRING)
  @Column(name = "current_state")
  @Setter(AccessLevel.NONE)
  @Builder.Default
  private Map<String, NegotiationResourceState> currentStatePerResource = new HashMap<>();

  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.ALL})
  @JoinColumn(name = "negotiation_id", referencedColumnName = "id")
  @Setter(AccessLevel.NONE)
  @Builder.Default
  private Set<NegotiationLifecycleRecord> lifecycleHistory = creteInitialHistory();

  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.ALL})
  @JoinColumn(name = "negotiation_id", referencedColumnName = "id")
  @Setter(AccessLevel.NONE)
  @Builder.Default
  private Set<NegotiationResourceLifecycleRecord> negotiationResourceLifecycleRecords =
      new HashSet<>();

  private static Set<NegotiationLifecycleRecord> creteInitialHistory() {
    Set<NegotiationLifecycleRecord> history = new HashSet<>();
    history.add(NegotiationLifecycleRecord.builder().changedTo(NegotiationState.SUBMITTED).build());
    return history;
  }

  public void setCurrentState(NegotiationState negotiationState) {
    this.currentState = negotiationState;
    this.lifecycleHistory.add(NegotiationLifecycleRecord.builder().changedTo(currentState).build());
  }

  public void setStateForResource(String resourceId, NegotiationResourceState state) {
    currentStatePerResource.put(resourceId, state);
    if (!state.equals(NegotiationResourceState.SUBMITTED)) {
      NegotiationResourceLifecycleRecord record =
          NegotiationResourceLifecycleRecord.builder()
              .recordedAt(LocalDateTime.now())
              .changedTo(state)
              .resource(lookupResource(getResources(), resourceId))
              .build();
      this.negotiationResourceLifecycleRecords.add(record);
    }
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

  public Set<Resource> getResources() {
    return requests.stream()
        .flatMap(request -> request.getResources().stream())
        .collect(Collectors.toUnmodifiableSet());
  }

  private Resource lookupResource(Set<Resource> resources, String resourceId) {
    return resources.stream()
        .filter(r -> r.getSourceId().equals(resourceId))
        .findFirst()
        .orElse(null);
  }
}
