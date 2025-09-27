package com.furb.folha.cliente.service;

import com.furb.folha.cliente.dto.HoleriteRec;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ClienteConsumer {

//    @RabbitListener(queues = "#{queueEventosCliente.name}")
//    public void consumirStatus(HoleriteRec holeriteRec) {
//        if (holeriteRec.salarioBruto() == 0) {
//            System.out.println("Status do cliente: " + holeriteRec.status());
//        } else {
//            System.out.println("=== Calculo Salário finalizado === ");
//            System.out.println("Salário Bruto: " + holeriteRec.salarioBruto());
//            System.out.println("INSS: " + holeriteRec.inss());
//            System.out.println("IRRF: " + holeriteRec.irrf());
//            System.out.println("Salário Liquído: " + holeriteRec.salarioLiquido());
//        }
//    }
}
