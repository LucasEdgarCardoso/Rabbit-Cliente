package com.furb.folha.cliente.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClienteNotificationConsumer {

    private final RabbitTemplate rabbitTemplate;
    private String clienteId;
    private String nomeFilaTemp;
    private volatile boolean consumerAtivo = false;

    public ClienteNotificationConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Configura o clienteId e inicia o polling manual das mensagens
     */
    public void configurarEIniciarConsumer(String id) {
        this.clienteId = id;
        this.nomeFilaTemp = "tmp.cliente.eventos." + clienteId;
        System.out.println("[DEBUG] ClienteId configurado no consumer: " + id);
        System.out.println("[DEBUG] Fila configurada: " + nomeFilaTemp);

        // Verificar se a fila realmente existe antes de iniciar
        if (verificarFilaExiste()) {
            consumerAtivo = true;
            iniciarPollingMensagens();
            System.out.println("[DEBUG] Consumer iniciado com sucesso!");
        } else {
            System.out.println("[AVISO] Fila nao encontrada. Consumer nao sera iniciado.");
        }
    }

    private boolean verificarFilaExiste() {
        try {
            // Tenta uma operação simples para verificar se a fila existe
            rabbitTemplate.receiveAndConvert(nomeFilaTemp, 500);
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("NOT_FOUND")) {
                return false;
            }
            return true; // Se erro diferente, assume que existe
        }
    }

    /**
     * SOLUÇÃO ROBUSTA: Polling com retry inteligente
     */
    private void iniciarPollingMensagens() {
        Thread consumerThread = new Thread(() -> {
            int tentativasSemMensagem = 0;
            boolean filaIndisponivel = false;

            while (consumerAtivo) {
                try {
                    // Tenta receber mensagem da fila
                    Object mensagem = rabbitTemplate.receiveAndConvert(nomeFilaTemp, 2000);

                    if (mensagem != null) {
                        receberNotificacao(mensagem.toString());
                        tentativasSemMensagem = 0;
                        filaIndisponivel = false;
                    } else {
                        tentativasSemMensagem++;
                    }

                    // Intervalo baseado na atividade
                    if (tentativasSemMensagem < 10) {
                        Thread.sleep(1000); // Polling ativo por 10 segundos
                    } else if (tentativasSemMensagem < 60) {
                        Thread.sleep(3000); // Polling moderado por 3 minutos
                    } else {
                        Thread.sleep(10000); // Polling baixo após 3 minutos
                    }

                } catch (Exception e) {
                    String errorMsg = e.getMessage();

                    if (errorMsg != null && errorMsg.contains("NOT_FOUND")) {
                        if (!filaIndisponivel) {
                            System.out.println("[AVISO] Fila temporariamente indisponivel. Aguardando...");
                            filaIndisponivel = true;
                        }
                        try {
                            Thread.sleep(10000); // Aguarda 10 segundos se fila não existe
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else if (errorMsg != null &&
                            !errorMsg.contains("Connection refused") &&
                            !errorMsg.contains("timed out") &&
                            !errorMsg.contains("null")) {
                        System.err.println("[ERRO] Falha no consumer: " + errorMsg);
                        try {
                            Thread.sleep(5000); // Pausa em caso de erro
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            System.out.println("[DEBUG] Thread do consumer finalizada");
        });

        consumerThread.setDaemon(true);
        consumerThread.setName("ClienteConsumer-" + clienteId);
        consumerThread.start();
    }

    /**
     * Processa as mensagens recebidas
     */
    public void receberNotificacao(String mensagem) {
        try {
            System.out.println("\n==============================================");
            System.out.println("🔔 [NOTIFICACAO RECEBIDA PARA " + clienteId + "]");

            if (mensagem.contains("\"status\":\"calculado\"")) {
                System.out.println("📊 Status: Calculo de impostos concluido!");
                exibirResultadoCalculo(mensagem);
            }
            else if (mensagem.contains("\"status\":\"salvo\"")) {
                System.out.println("💾 Status: Dados salvos no banco de dados!");
                exibirResultadoFinal(mensagem);

                System.out.println("==============================================");
                System.out.print("Digite seu salario (ou 'sair' para encerrar): R$ ");
            }
            else {
                System.out.println("📄 Dados recebidos: " + mensagem);
            }



        } catch (Exception e) {
            System.out.println("❌ Erro ao processar notificacao: " + e.getMessage());
        }
    }

    private void exibirResultadoCalculo(String mensagem) {
        try {
            String salarioBruto = extrairValor(mensagem, "salarioBruto");
            String inss = extrairValor(mensagem, "inss");
            String irrf = extrairValor(mensagem, "irrf");
            String salarioLiquido = extrairValor(mensagem, "salarioLiquido");

            System.out.println("💰 Salario Bruto: R$ " + salarioBruto);
            System.out.println("📉 INSS: R$ " + inss);
            System.out.println("📉 IRRF: R$ " + irrf);
            System.out.println("💵 Salario Liquido: R$ " + salarioLiquido);
        } catch (Exception e) {
            System.out.println("📄 Dados: " + mensagem);
        }
    }

    private void exibirResultadoFinal(String mensagem) {
        try {
            String id = extrairValor(mensagem, "id");
            String dataProcessamento = extrairValor(mensagem, "dataProcessamento");
            String salarioLiquido = extrairValor(mensagem, "salarioLiquido");

            System.out.println("🆔 ID do Registro: " + id);
            System.out.println("📅 Data/Hora: " + dataProcessamento.replace("\"", ""));
            System.out.println("💵 Valor Final: R$ " + salarioLiquido);
        } catch (Exception e) {
            System.out.println("📄 Dados: " + mensagem);
        }
    }

    private String extrairValor(String json, String campo) {
        String busca = "\"" + campo + "\":";
        int inicio = json.indexOf(busca);
        if (inicio == -1) return "N/A";

        inicio += busca.length();
        int fim = json.indexOf(",", inicio);
        if (fim == -1) fim = json.indexOf("}", inicio);

        return json.substring(inicio, fim).replace("\"", "").trim();
    }

    /**
     * Para parar o consumer se necessário
     */
    public void pararConsumer() {
        consumerAtivo = false;
    }
}