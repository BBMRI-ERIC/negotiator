package eu.bbmri_eric.negotiator.negotiation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NegotiationUpdateLifecycleDTO {
  @Nullable private String details;
}
