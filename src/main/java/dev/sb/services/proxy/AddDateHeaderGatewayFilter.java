package dev.sb.services.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Adds "Date" HTTP header to request headers if not set */
@Slf4j
public class AddDateHeaderGatewayFilter implements GatewayFilter {

    public AddDateHeaderGatewayFilter() {
        
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!request.getHeaders().containsKey(ProxyHeaders.DATE)) {
            String isoDate = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME);
            if (log.isTraceEnabled()) {
                log.trace("HTTP header 'Date':'{}'", isoDate);
            }
            request = request.mutate().header(ProxyHeaders.DATE, isoDate).build();
        }
        return chain.filter(exchange.mutate().request(request).build());
    }
}
