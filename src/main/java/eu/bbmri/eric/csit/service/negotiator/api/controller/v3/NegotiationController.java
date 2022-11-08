package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v3")
public class NegotiationController {

    private final NegotiationServiceImpl negotiationServiceImpl = new NegotiationServiceImpl();

    @PostMapping(
            value = "/negotiations",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    RequestDTO createNegotiation(@Valid @RequestBody NegotiationCreateDTO negotiationCreateDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Person creator = ((NegotiatorUserDetails) auth.getPrincipal()).getPerson();
        negotiationServiceImpl.startNegotiation(null, creator.getId());
        return null;
    }
}
