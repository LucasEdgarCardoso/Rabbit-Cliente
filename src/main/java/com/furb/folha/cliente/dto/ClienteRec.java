package com.furb.folha.cliente.dto;

import java.util.UUID;

public record ClienteRec(
        UUID id,
        double salario
) {}
