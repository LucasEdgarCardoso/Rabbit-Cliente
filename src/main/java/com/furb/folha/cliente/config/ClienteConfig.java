package com.furb.folha.cliente.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClienteConfig {

    private TopicExchange topicExchange;

    @Autowired
    private AmqpAdmin amqpAdmin;

    // Métod para criar a fila com o clienteId dinâmico
    public void createQueue(String clienteId) {
        // Cria a fila com o nome dinâmico baseado no clienteId
        Queue queue = new Queue("tmp.cliente.eventos." + clienteId, true);
        amqpAdmin.declareQueue(queue); // Registra a fila no RabbitMQ
    }

    @Bean
    public TopicExchange exchange() {
        topicExchange = new TopicExchange("eventos.topic");
        return topicExchange;
    }

    // Binding para a fila de eventos do cliente
    @Bean
    public Binding bindingEventosCliente(Queue queueEventosCliente, TopicExchange exchange) {
        return BindingBuilder.bind(queueEventosCliente).to(exchange).with("cliente.#");
    }
}
