package com.furb.folha.cliente;

import com.furb.folha.cliente.config.ClienteConfig;
import com.furb.folha.cliente.dto.ClienteRec;
import com.furb.folha.cliente.service.ClienteProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;
import java.util.UUID;

@SpringBootApplication
public class ClienteApplication implements CommandLineRunner {

    @Autowired
    private ClienteProducer clienteProducer;


    @Autowired
    private ClienteConfig clienteConfig;

    public static void main(String[] args) {
        SpringApplication.run(ClienteApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Solicita o salário ao usuário
        System.out.print("Informe seu salário: ");
        double salario = scanner.nextDouble();

        // Gera um UUID único para o cliente
        UUID clienteId = UUID.randomUUID();

        // Cria a fila para o cliente
        clienteConfig.createQueue(clienteId.toString());  // Chama o métod para criar a fila com o clienteId

        // Envia o salário para o processamento
        clienteProducer.enviarSalario(new ClienteRec(clienteId, salario));

        System.out.println("Salário enviado para o processamento com o ID: " + clienteId);
    }

}
