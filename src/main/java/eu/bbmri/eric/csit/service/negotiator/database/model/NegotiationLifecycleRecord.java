package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.time.ZonedDateTime;
import lombok.*;

public record NegotiationLifecycleRecord(
    ZonedDateTime recordTime, Person madeBy, NegotiationState newState) {
  @Override
  public ZonedDateTime recordTime() {
    return recordTime;
  }

  @Override
  public Person madeBy() {
    return madeBy;
  }

  @Override
  public NegotiationState newState() {
    return newState;
  }
}
