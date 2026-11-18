package com.rapipedido.tracking.interno;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class TrackingConfig implements WebSocketMessageBrokerConfigurer {

    public static final String EXCHANGE = "rapipedido.tracking";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registro) {
        registro.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registro) {
        registro.enableSimpleBroker("/topic");
        registro.setApplicationDestinationPrefixes("/app");
    }

    @Bean
    public FanoutExchange trackingExchange() {
        return new FanoutExchange(EXCHANGE, false, false);
    }

    /** Cola exclusiva de esta réplica; desaparece cuando la réplica se apaga. */
    @Bean
    public AnonymousQueue colaTrackingInstancia() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding trackingBinding() {
        return BindingBuilder.bind(colaTrackingInstancia()).to(trackingExchange());
    }
}
