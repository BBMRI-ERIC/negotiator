package eu.bbmri_eric.negotiator.common;

import java.util.List;
import java.util.Objects;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@CommonsLog
public class AuthorizationServiceImpl implements AuthorizationService {

  @Override
  public void checkAuthorization(Long personId) {
    final List<String> roles = AuthenticatedUserContext.getRoles();
    if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_AUTHORIZATION_MANAGER")) {
      return;
    }
    final Long currentId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    if (!Objects.equals(currentId, personId)) {
      log.warn("Forbidden access: user " + currentId + " attempted to act on user " + personId);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }
}
