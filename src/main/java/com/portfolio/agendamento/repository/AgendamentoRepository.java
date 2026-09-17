package com.portfolio.agendamento.repository;

import com.portfolio.agendamento.model.Agendamento;
import com.portfolio.agendamento.model.Agendamento.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("""
        SELECT COUNT(a) > 0 FROM Agendamento a
        WHERE a.chacara.id = :chacaraId
          AND a.status = :status
          AND a.dataReserva <= :dataFim
          AND a.dataFim >= :dataInicio
    """)
    boolean existeConflito(
        @Param("chacaraId") Long chacaraId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        @Param("status") StatusAgendamento status
    );

    @Query("""
        SELECT COUNT(a) > 0 FROM Agendamento a
        WHERE a.chacara.id = :chacaraId
          AND a.id != :id
          AND a.status = :status
          AND a.dataReserva <= :dataFim
          AND a.dataFim >= :dataInicio
    """)
    boolean existeConflitoExcluindoId(
        @Param("chacaraId") Long chacaraId,
        @Param("id") Long id,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        @Param("status") StatusAgendamento status
    );

    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.chacara.id = :chacaraId
          AND a.status = :status
          AND a.dataReserva <= :fim
          AND a.dataFim >= :inicio
        ORDER BY a.dataReserva ASC
    """)
    List<Agendamento> findConflitosNoPeriodo(
        @Param("chacaraId") Long chacaraId,
        @Param("status") StatusAgendamento status,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    List<Agendamento> findByUsuarioIdOrderByDataReservaDesc(Long usuarioId);

    default boolean existsByChacaraIdAndDataReservaAndStatus(
        Long chacaraId, LocalDate dataReserva, StatusAgendamento status
    ) {
        return existeConflito(chacaraId, dataReserva, dataReserva, status);
    }

    default boolean existsByChacaraIdAndDataReservaAndIdNotAndStatus(
        Long chacaraId, LocalDate dataReserva, Long id, StatusAgendamento status
    ) {
        return existeConflitoExcluindoId(chacaraId, id, dataReserva, dataReserva, status);
    }
}
