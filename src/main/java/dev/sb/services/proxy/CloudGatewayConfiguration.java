package dev.sb.services.proxy;

import dev.sb.services.signing.RequestSigner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CloudGatewayConfiguration {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               RequestSigner signingFilter,
                               @Value("${application.gateway.uri}") String gatewayUri,
                               @Value("${application.gateway.path}") String gatewayPath,
                               @Value("${application.proxy.path}") String proxyPath) {

        log.info("Configuring routing: {}/** -> {}{}", proxyPath, gatewayUri, gatewayPath);

        return builder.routes()
            // Postman Echo route is used for testing and debug
            // please do not expose sensitive info through it
            .route("signing_route", r -> r
                .path(proxyPath + "/**")
                .filters(f -> f
                    .addRequestHeader(ProxyHeaders.X_COMPANY_SERVICE_NAME, "signing-proxy")
                    .rewritePath(proxyPath + "/(?<segment>.*)", gatewayPath + "/${segment}")
                    .filter(new AddDateHeaderGatewayFilter(), 0)
                    .filter(new AddDigestHeaderGatewayFilter(signingFilter), 1)
                    .filter(new AddSignatureHeaderGatewayFilter(signingFilter), 2))
                .uri(gatewayUri))
            .build();
    }
}
