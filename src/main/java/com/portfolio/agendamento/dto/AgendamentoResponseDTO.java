package com.portfolio.agendamento.dto;

import com.portfolio.agendamento.model.Agendamento;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AgendamentoResponseDTO {

    private Long id;
    private Long chacaraId;
    private String chacaraNome;
    private Long usuarioId;
    private String nomeUsuario;
    private String emailUsuario;
    private String nomeCliente;
    private String telefoneCliente;
    private LocalDate dataReserva;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private int totalDias;
    private String periodoFormatado;
    private Agendamento.StatusAgendamento status;

    public static AgendamentoResponseDTO fromEntity(Agendamento a) {
        LocalDate inicio = a.getDataInicio() != null ? a.getDataInicio() : a.getDataReserva();
        LocalDate fim = a.getDataFim() != null ? a.getDataFim() : inicio;
        int dias = (int) java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1;

        String periodo;
        if (inicio.equals(fim)) {
            periodo = String.format("%02d/%02d/%d (1 diária)", inicio.getDayOfMonth(), inicio.getMonthValue(), inicio.getYear());
        } else {
            periodo = String.format("%02d/%02d a %02d/%02d/%d (%d diárias)",
                    inicio.getDayOfMonth(), inicio.getMonthValue(),
                    fim.getDayOfMonth(), fim.getMonthValue(), fim.getYear(), dias);
        }

        return new AgendamentoResponseDTO(
                a.getId(),
                a.getChacara().getId(),
                a.getChacara().getNome(),
                a.getUsuario().getId(),
                a.getUsuario().getNomeCompleto(),
                a.getUsuario().getEmail(),
                a.getNomeCliente(),
                a.getTelefoneCliente(),
                inicio,
                inicio,
                fim,
                dias,
                periodo,
                a.getStatus()
        );
    }
}