package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri.eric.csit.service.negotiator.service.MolgenisServiceImplementation;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@WireMockTest(httpPort = 8080)
public class MolgenisServiceTest {

  @Test
  void isReachable_nullUrl_False() {
    assertThrows(NullPointerException.class, () -> new MolgenisServiceImplementation(null));
  }

  @Test
  void isReachable_emptyString_False() {
    assertFalse(new MolgenisServiceImplementation(WebClient.create("")).isReachable());
  }

  @Test
  void isReachable_correctUrl_True() {
    stubFor(
        get(urlEqualTo("/directory"))
            .willReturn(aResponse().withBody("Directory is Up and Running!")));
    assertTrue(
        new MolgenisServiceImplementation(WebClient.create("http://localhost:8080/directory"))
            .isReachable());
  }
}
