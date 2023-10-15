package eu.bbmri_eric.negotiator.database.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.*;
import lombok.ToString.Exclude;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@ToString
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
  @Column(columnDefinition = "jsonb")
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

  private static Set<NegotiationLifecycleRecord> creteInitialHistory() {
    Set<NegotiationLifecycleRecord> history = new HashSet<>();
    history.add(
        NegotiationLifecycleRecord.builder()
            .recordedAt(ZonedDateTime.now())
            .changedTo(NegotiationState.SUBMITTED)
            .build());
    return history;
  }

  public void setCurrentState(NegotiationState negotiationState) {
    this.currentState = negotiationState;
    this.lifecycleHistory.add(
        NegotiationLifecycleRecord.builder()
            .recordedAt(ZonedDateTime.now())
            .changedTo(currentState)
            .build());
  }

  public void setStateForResource(String resourceId, NegotiationResourceState state) {
    currentStatePerResource.put(resourceId, state);
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

  public Set<Resource> getAllResources() {
    return requests.stream()
        .flatMap(request -> request.getResources().stream())
        .collect(Collectors.toUnmodifiableSet());
  }
}
