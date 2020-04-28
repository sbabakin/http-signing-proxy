package dev.sb.services.proxy;

import dev.sb.services.HttpSigningProxyApplication;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.Security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * All tests have following flow:
 * (client) --> (this signing proxy app)--> (mocked third-party gateway)
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = HttpSigningProxyApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@ContextConfiguration
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
@Slf4j
public class CloudGatewayConfigurationTest {

    @Autowired
    private WebTestClient webTestClient;

    private ClientAndServer gatewayMockServer;

    @BeforeAll
    public static void beforeClass() throws IOException {
        // register BC provider
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeEach
    public void startMockServer() {
        gatewayMockServer = ClientAndServer.startClientAndServer(22223);
    }

    @AfterEach
    public void stopMockServer() {
        gatewayMockServer.stop();
    }

    @Test
    @DisplayName("happy pass GET: HTTP 200")
    public void proxyGetMethod() {
        // this replies on signed request
        gatewayMockServer
            .when(request()
                .withMethod("GET")
                .withPath("/third-party/api/get"))
            .respond(response()
                .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                .withBody("{\"response\": \"value\"}")
                .withStatusCode(200));

        // this calls signing proxy
        webTestClient.get().uri("/api/get")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.response").isEqualTo("value");

        HttpRequest[] httpRequests = gatewayMockServer.retrieveRecordedRequests(request().withMethod("GET"));
        assertThat(httpRequests, is(notNullValue()));

        HttpRequest httpRequest = httpRequests[0];
        assertThat(httpRequest.getHeader(ProxyHeaders.X_COMPANY_SERVICE_NAME), hasItem("signing-proxy"));
        assertThat(httpRequest.getHeader(ProxyHeaders.DATE), hasItem(notNullValue()));
        // we do not calculate Digest for GET methods (no body)
        assertThat(httpRequest.getHeader(ProxyHeaders.DIGEST), hasSize(0));
        // analyze signature in detail, note that signature header doesn't contain Digest
        assertThat(httpRequest.getHeader(ProxyHeaders.SIGNATURE), is(notNullValue()));
        String signature = httpRequest.getHeader(ProxyHeaders.SIGNATURE).get(0);
        assertThat(signature, matchesPattern("keyId=\"ABC12345\",algorithm=\"rsa-sha256\",headers=\"x-company-service-name date\",signature=\".*\""));
    }

    @Test
    @DisplayName("happy pass POST: HTTP 201")
    public void proxyPostMethod() {
        gatewayMockServer
            .when(request()
                .withMethod("POST")
                .withPath("/third-party/api/post"))
            .respond(response()
                .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                .withBody("{\"response\": \"value\"}")
                .withStatusCode(201));

        String requestBody = "{\"request\":\"value\"}";

        webTestClient.post().uri("/api/post")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Mono.just(requestBody), String.class)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.response").isEqualTo("value");

        HttpRequest[] httpRequests = gatewayMockServer.retrieveRecordedRequests(request().withMethod("POST"));
        assertThat(httpRequests, is(notNullValue()));

        HttpRequest httpRequest = httpRequests[0];
        // we calculate Digest fro POST methods
        assertThat(httpRequest.getHeader(ProxyHeaders.DIGEST), is(notNullValue()));
        String digest = httpRequest.getHeader(ProxyHeaders.DIGEST).get(0);
        assertThat(digest, is("SHA-256=bVR2BHJXhy8kEBxXKbGsLT0dB1qICnZQ1BtN5MaAKwg="));

        // analyze signature in detail
        assertThat(httpRequest.getHeader(ProxyHeaders.SIGNATURE), is(notNullValue()));
        String signature = httpRequest.getHeader(ProxyHeaders.SIGNATURE).get(0);
        assertThat(signature, matchesPattern("keyId=\"ABC12345\",algorithm=\"rsa-sha256\",headers=\"x-company-service-name date digest\",signature=\".*\""));
    }

    @Test
    @DisplayName("non existent route: HTTP 404")
    public void nonexistentRoute() {
        webTestClient.get().uri("/no_route")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound();
    }

}
