package eu.bbmri_eric.negotiator.dto.person;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OwnershipTransferDTO {

    private Long newOwnerId;
    private String newOwnerEmail;
}