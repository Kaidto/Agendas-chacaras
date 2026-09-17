package com.portfolio.agendamento.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AgendamentoUpdateDTO {

    @NotNull(message = "A chácara é obrigatória")
    private Long chacaraId;

    @FutureOrPresent(message = "A data da reserva deve ser hoje ou em uma data futura")
    private LocalDate dataReserva;

    @FutureOrPresent(message = "A data de início deve ser hoje ou em uma data futura")
    private LocalDate dataInicio;

    @FutureOrPresent(message = "A data de término deve ser hoje ou em uma data futura")
    private LocalDate dataFim;

    @NotBlank(message = "O nome do cliente é obrigatório")
    @Size(min = 2, max = 150, message = "O nome do cliente deve ter entre 2 e 150 caracteres")
    private String nomeCliente;

    @NotBlank(message = "O telefone do cliente é obrigatório")
    @Pattern(
            regexp = "^[0-9()\\-\\s+]{10,20}$",
            message = "Telefone inválido"
    )
    private String telefoneCliente;

    public LocalDate resolverDataInicio() {
        if (dataInicio != null) return dataInicio;
        return dataReserva;
    }

    public LocalDate resolverDataFim() {
        if (dataFim != null) return dataFim;
        if (dataInicio != null) return dataInicio;
        return dataReserva;
    }
}
