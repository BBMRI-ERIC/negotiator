package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;
import lombok.*;
import lombok.ToString.Exclude;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "negotiation")
@TypeDef(typeClass = JsonType.class, name = "json")
@NamedEntityGraph(
    name = "negotiation-with-detailed-children",
    attributeNodes = {
      @NamedAttributeNode(value = "persons", subgraph = "persons-with-roles"),
      @NamedAttributeNode(value = "requests", subgraph = "requests-detailed"),
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

  @ManyToMany(mappedBy = "negotiations")
  @Exclude
  Set<Attachment> attachments;

  @OneToMany(
      mappedBy = "negotiation",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
      fetch = FetchType.LAZY)
  @Exclude
  private Set<PersonNegotiationRole> persons = new HashSet<>();

  @OneToMany(mappedBy = "negotiation", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  @Exclude
  private Set<Request> requests;

  @Type(type = "json")
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

  public void setCurrentState(NegotiationState negotiationState) {
    this.currentState = negotiationState;
    this.lifecycleHistory.add(
        NegotiationLifecycleRecord.builder()
            .recordedAt(ZonedDateTime.now())
            .changedTo(currentState)
            .build());
  }

  private static Set<NegotiationLifecycleRecord> creteInitialHistory() {
    Set<NegotiationLifecycleRecord> history = new HashSet<>();
    history.add(
        NegotiationLifecycleRecord.builder()
            .recordedAt(ZonedDateTime.now())
            .changedTo(NegotiationState.SUBMITTED)
            .build());
    return history;
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
