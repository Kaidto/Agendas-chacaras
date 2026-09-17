package com.portfolio.agendamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chacara_id", nullable = false)
    private Chacara chacara;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_reserva", nullable = false)
    private LocalDate dataReserva; // Início do período (Check-in)

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim; // Fim do período (Check-out)

    @Column(name = "nome_cliente", nullable = false, length = 150)
    private String nomeCliente;

    @Column(name = "telefone_cliente", nullable = false, length = 20)
    private String telefoneCliente;

    public LocalDate getDataInicio() {
        return dataReserva;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataReserva = dataInicio;
        if (this.dataFim == null) {
            this.dataFim = dataInicio;
        }
    }

    @PrePersist
    @PreUpdate
    public void preSalvar() {
        if (this.dataFim == null && this.dataReserva != null) {
            this.dataFim = this.dataReserva;
        }
    }

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status = StatusAgendamento.CONFIRMADO;

    @Version
    private Long versao;

    public enum StatusAgendamento {
        CONFIRMADO,
        CANCELADO
    }
}