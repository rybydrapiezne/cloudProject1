package pl.edu.pwr.chat.config

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter


import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtPropagationFilter : GlobalFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val jwt = request.headers.getFirst("Authorization")

        return if (jwt != null && jwt.startsWith("Bearer ")) {
            chain.filter(exchange.mutate().request(
                request.mutate()
                    .header("Authorization", jwt)
                    .build()
            ).build())
        } else {
            chain.filter(exchange)
        }
    }
}