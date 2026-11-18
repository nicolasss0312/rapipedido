package com.rapipedido.eventos.interno;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapipedido.eventos.Eventos;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(Eventos.EXCHANGE, true, false);
    }

    /** Los mensajes que agotan sus reintentos terminan aquí para revisión manual. */
    @Bean
    public FanoutExchange eventosDlx() {
        return new FanoutExchange(Eventos.EXCHANGE_DLX, true, false);
    }

    @Bean
    public Queue eventosDlq() {
        return QueueBuilder.durable(Eventos.COLA_DLQ).build();
    }

    @Bean
    public Binding eventosDlqBinding() {
        return BindingBuilder.bind(eventosDlq()).to(eventosDlx());
    }

    /**
     * Convierte el JSON al tipo del parámetro del {@code @RabbitListener}; así los módulos no
     * dependen de nombres de clase en los encabezados del mensaje.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter conversor = new Jackson2JsonMessageConverter(objectMapper);
        conversor.setAlwaysConvertToInferredType(true);
        return conversor;
    }
}
