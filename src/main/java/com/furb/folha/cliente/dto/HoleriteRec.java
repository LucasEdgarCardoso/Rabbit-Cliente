package com.furb.folha.cliente.dto;

import java.util.UUID;

public record HoleriteRec(
        UUID id,
        String status,
        double salarioBruto,
        double inss,
        double irrf,
        double salarioLiquido
) {}
