package dev.sb.services.proxy;

import dev.sb.services.signing.RequestSigner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Calculate and add HTTP message signature to the request headers.
 */
@Slf4j
public class AddSignatureHeaderGatewayFilter implements GatewayFilter {

    private final RequestSigner requestSigner;

    public AddSignatureHeaderGatewayFilter(RequestSigner requestSigner) {
        this.requestSigner = requestSigner;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String method = request.getMethodValue();
        String path = request.getPath().toString();

        if (log.isTraceEnabled()) {
            log.trace("(request-target): '{} {}'", method, path);
        }

        // now take all the headers and create signature
        String signatureHeader = requestSigner.createSignature(method, path, request.getHeaders().toSingleValueMap());
        request = request.mutate().header(ProxyHeaders.SIGNATURE, signatureHeader).build();

        if (log.isTraceEnabled()) {
            log.trace("HTTP headers: {}", request.getHeaders().toString());
        }
        return chain.filter(exchange.mutate().request(request).build());
    }
}
