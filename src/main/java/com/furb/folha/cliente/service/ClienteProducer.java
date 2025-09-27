package com.furb.folha.cliente.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furb.folha.cliente.dto.ClienteRec;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClienteProducer {

    private final AmqpTemplate amqpTemplate;

    public ClienteProducer(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void enviarSalario(ClienteRec clienteRec) {

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            var jsonEnvio = objectMapper.writeValueAsString(clienteRec);
            amqpTemplate.convertAndSend("calc.direct", "requisicao.salario", jsonEnvio);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
