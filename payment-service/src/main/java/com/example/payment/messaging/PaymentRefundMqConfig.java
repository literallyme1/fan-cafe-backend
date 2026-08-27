package com.example.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.payment.messaging.PaymentRefundMqNames.COMMAND_QUEUE;
import static com.example.payment.messaging.PaymentRefundMqNames.COMMAND_ROUTING_KEY;
import static com.example.payment.messaging.PaymentRefundMqNames.EXCHANGE;

@Configuration
public class PaymentRefundMqConfig {

    @Bean
    public DirectExchange paymentRefundExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue paymentRefundCommandQueue() {
        return QueueBuilder.durable(COMMAND_QUEUE).build();
    }

    @Bean
    public Binding paymentRefundCommandBinding(
            @Qualifier("paymentRefundCommandQueue") Queue queue,
            @Qualifier("paymentRefundExchange") DirectExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(COMMAND_ROUTING_KEY);
    }

    @Bean
    public Queue paymentApprovalCommandQueue() {
        return QueueBuilder.durable(PaymentRefundMqNames.APPROVAL_COMMAND_QUEUE).build();
    }

    @Bean
    public Binding paymentApprovalCommandBinding(
            @Qualifier("paymentApprovalCommandQueue") Queue queue,
            @Qualifier("paymentRefundExchange") DirectExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange)
                .with(PaymentRefundMqNames.APPROVAL_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter paymentMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate paymentRabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter paymentMessageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(paymentMessageConverter);
        return template;
    }
}
