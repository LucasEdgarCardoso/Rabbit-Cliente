package com.furb.folha.cliente;

import com.furb.folha.cliente.service.ClienteNotificationConsumer;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;
import java.util.UUID;
import java.util.Locale;

@SpringBootApplication
public class ClienteApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ClienteApplication.class, args);
    }

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final ClienteNotificationConsumer consumer;
    private final String clienteId;

    public ClienteApplication(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin, ClienteNotificationConsumer consumer) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
        this.consumer = consumer;
        this.clienteId = "cliente-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== CLIENTE CALCULO DE SALARIO ===");
        System.out.println("Cliente ID: " + clienteId);
        System.out.println("==================================");

        // Criar fila temporária do cliente
        criarFilaTemporariaCliente();

        System.out.println("Aguardando estabilizacao das filas...");
        Thread.sleep(3000); // Aguarda mais tempo para CloudAMQP

        // Configurar consumer com verificação se a fila existe
        if (verificarSeFilaExiste()) {
            consumer.configurarEIniciarConsumer(clienteId);
            System.out.println("Consumer configurado com sucesso!");
        } else {
            System.out.println("AVISO: Fila nao encontrada. Consumer nao sera iniciado.");
            System.out.println("O sistema funcionara, mas sem notificacoes em tempo real.");
        }

        while (true) {
            System.out.print("Digite seu salario (ou 'sair' para encerrar): R$ ");
            String input = scanner.nextLine();

            if ("sair".equalsIgnoreCase(input)) {
                System.out.println("Encerrando...");
                break;
            }

            try {
                double salario = Double.parseDouble(input);
                enviarSalarioParaProcessamento(salario);
                System.out.println("Salario enviado! Aguarde o processamento...");

            } catch (NumberFormatException e) {
                System.out.println("Por favor, digite um valor valido!");
            }
        }
    }

    private void criarFilaTemporariaCliente() {
        try {
            // CORREÇÃO: Fila NÃO exclusiva para CloudAMQP
            String nomeFilaTemp = "tmp.cliente.eventos." + clienteId;
            Queue filaTemp = new Queue(nomeFilaTemp, false, false, false); // durável=false, exclusiva=false, auto-delete=false

            amqpAdmin.declareQueue(filaTemp);

            // Aguardar um pouco para a fila ser realmente criada
            Thread.sleep(1000);

            // Binding para receber eventos direcionados ao cliente
            TopicExchange eventosExchange = new TopicExchange("eventos.topic");
            Binding bindingCliente = new Binding(
                    nomeFilaTemp,
                    Binding.DestinationType.QUEUE,
                    "eventos.topic",
                    clienteId, // routing key = cliente.ID
                    null
            );

            amqpAdmin.declareBinding(bindingCliente);

            System.out.println("Fila temporaria criada: " + nomeFilaTemp);
            System.out.println("Binding criado para routing key: " + clienteId);

        } catch (Exception e) {
            System.err.println("ERRO ao criar fila: " + e.getMessage());
        }
    }

    private boolean verificarSeFilaExiste() {
        try {
            String nomeFilaTemp = "tmp.cliente.eventos." + clienteId;
            // Tenta fazer uma operação simples na fila para verificar se existe
            rabbitTemplate.receiveAndConvert(nomeFilaTemp, 100);
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("NOT_FOUND")) {
                return false;
            }
            return true; // Se erro diferente, assume que existe
        }
    }

    private void enviarSalarioParaProcessamento(double salario) {
        // CORREÇÃO: Usar formatação americana (ponto decimal) para JSON válido
        String mensagem = String.format(Locale.US, "{\"clienteId\":\"%s\", \"salario\":%f}", clienteId, salario);

        rabbitTemplate.convertAndSend(
                "calc.direct",
                "requisicao.salario",
                mensagem
        );

        System.out.println("Mensagem enviada: " + mensagem);
    }
}