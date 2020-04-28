package dev.sb.services.proxy;

import dev.sb.services.signing.RequestSigner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Calculate HTTP message Digest */
@Slf4j
public class AddDigestHeaderGatewayFilter implements GatewayFilter {
    
    private static final List<String> METHODS = List.of("POST", "PUT");

    private final RequestSigner requestSigner;

    public AddDigestHeaderGatewayFilter(RequestSigner requestSigner) {
        this.requestSigner = requestSigner;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (METHODS.contains(exchange.getRequest().getMethodValue())) {

            return ServerWebExchangeUtils.cacheRequestBody(exchange,
                (serverHttpRequest) -> {
                    // here we obtain decorated serverHttpRequest and read body value from exchange attribute
                    DataBuffer dataBuffer = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);

                    if (dataBuffer == null) {
                        return chain.filter(exchange);
                    }

                    String body = dataBuffer.toString(StandardCharsets.UTF_8);
                    String digest = requestSigner.createDigest(body);
                    if (log.isTraceEnabled()) {
                        log.trace("HTTP message body  : {}", body);
                        log.trace("HTTP message Digest: {}", digest);
                    }

                    serverHttpRequest.mutate().header(ProxyHeaders.DIGEST, digest);
                    return chain.filter(exchange.mutate().request(serverHttpRequest).build());
                });
        } else {
            return chain.filter(exchange);
        }
    }
}
