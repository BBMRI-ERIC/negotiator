package eu.bbmri_eric.negotiator.post;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A role tag annotating the author of a post")
public record PostTagDTO(
    @Schema(description = "Stable machine-readable key of the tag", example = "is-representative")
        String key,
    @Schema(description = "Human-readable name of the tag", example = "Representative") String name,
    @Schema(description = "Description of the tag", example = "Representative of a resource")
        String description) {}
