package eu.bbmri_eric.negotiator.governance.network;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Parameters to filter Network statistics")
public class NetworkStatsFilter {
  @NotNull
  @Schema(description = "Start of period", example = "2023-01-01")
  LocalDate since;

  @NotNull
  @Schema(description = "End of period", example = "2024-11-18")
  LocalDate until;
}
