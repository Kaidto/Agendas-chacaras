package com.portfolio.agendamento.service;

import com.portfolio.agendamento.exception.ConflitoHorarioException;
import com.portfolio.agendamento.model.Agendamento;
import com.portfolio.agendamento.model.Agendamento.StatusAgendamento;
import com.portfolio.agendamento.model.Chacara;
import com.portfolio.agendamento.model.Role;
import com.portfolio.agendamento.model.Usuario;
import com.portfolio.agendamento.repository.AgendamentoRepository;
import com.portfolio.agendamento.repository.ChacaraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AgendamentoService {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);

    private final AgendamentoRepository agendamentoRepository;
    private final ChacaraRepository chacaraRepository;

    public AgendamentoService(
            AgendamentoRepository agendamentoRepository,
            ChacaraRepository chacaraRepository) {

        this.agendamentoRepository = agendamentoRepository;
        this.chacaraRepository = chacaraRepository;
    }

    /**
     * Cria um novo agendamento de diárias consecutivas para a chácara (estilo Airbnb).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Agendamento agendar(
            Long chacaraId,
            Usuario usuario,
            LocalDate dataInicio,
            LocalDate dataFim,
            String nomeCliente,
            String telefoneCliente) {

        if (dataInicio == null) {
            throw new IllegalArgumentException("Data inicial é obrigatória");
        }
        if (dataFim == null) {
            dataFim = dataInicio;
        }

        if (dataInicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Não é possível agendar em uma data passada");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à data inicial");
        }

        Chacara chacara = chacaraRepository.findById(chacaraId)
                .orElseThrow(() -> new IllegalArgumentException("Chácara não encontrada"));

        boolean jaOcupado = agendamentoRepository.existeConflito(
                chacaraId,
                dataInicio,
                dataFim,
                StatusAgendamento.CONFIRMADO
        );

        if (jaOcupado) {
            throw new ConflitoHorarioException(
                    "A chácara " + chacara.getNome() + " já possui uma reserva confirmada que coincide com o período selecionado. Por favor, escolha outras datas livres."
            );
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setChacara(chacara);
        agendamento.setUsuario(usuario);
        agendamento.setDataReserva(dataInicio);
        agendamento.setDataFim(dataFim);
        agendamento.setNomeCliente(nomeCliente);
        agendamento.setTelefoneCliente(telefoneCliente);
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);

        try {
            return agendamentoRepository.save(agendamento);
        } catch (DataIntegrityViolationException e) {
            log.error("[AgendamentoService] Falha de integridade ao salvar agendamento: {}", e.getMessage());
            throw new ConflitoHorarioException(
                    "Este período acabou de ser reservado por outro usuário. Atualize o calendário e escolha outra data."
            );
        }
    }

    /**
     * Sobrecarga para reserva de data única.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Agendamento agendar(
            Long chacaraId,
            Usuario usuario,
            LocalDate data,
            String nomeCliente,
            String telefoneCliente) {
        return agendar(chacaraId, usuario, data, data, nomeCliente, telefoneCliente);
    }

    /**
     * Atualiza dados de um agendamento existente (período de datas, cliente, telefone ou chácara).
     */
    @Transactional
    public Agendamento atualizar(
            Long id,
            Long novaChacaraId,
            Usuario usuarioLogado,
            LocalDate novaDataInicio,
            LocalDate novaDataFim,
            String nomeCliente,
            String telefoneCliente) {

        if (novaDataInicio == null) {
            throw new IllegalArgumentException("Data inicial é obrigatória");
        }
        if (novaDataFim == null) {
            novaDataFim = novaDataInicio;
        }

        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        boolean isDono = agendamento.getUsuario().getId().equals(usuarioLogado.getId());
        boolean isAdmin = usuarioLogado.getRoles().contains(Role.ROLE_ADMIN);

        if (!isDono && !isAdmin) {
            throw new SecurityException("Você não tem permissão para alterar este agendamento");
        }

        if (novaDataInicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Não é possível alterar para uma data que já passou");
        }
        if (novaDataFim.isBefore(novaDataInicio)) {
            throw new IllegalArgumentException("A data final deve ser posterior ou igual à data inicial");
        }

        Chacara chacara = chacaraRepository.findById(novaChacaraId)
                .orElseThrow(() -> new IllegalArgumentException("Chácara não encontrada"));

        // Se datas ou chácara mudaram, valida se há conflito excluindo o agendamento atual
        boolean mudou = !agendamento.getDataReserva().equals(novaDataInicio)
                || !agendamento.getDataFim().equals(novaDataFim)
                || !agendamento.getChacara().getId().equals(novaChacaraId);

        if (mudou) {
            boolean jaOcupado = agendamentoRepository.existeConflitoExcluindoId(
                    novaChacaraId,
                    id,
                    novaDataInicio,
                    novaDataFim,
                    StatusAgendamento.CONFIRMADO
            );

            if (jaOcupado) {
                throw new ConflitoHorarioException(
                        "A chácara " + chacara.getNome() + " já possui uma reserva confirmada para o período informado. Escolha outras datas."
                );
            }
        }

        agendamento.setChacara(chacara);
        agendamento.setDataReserva(novaDataInicio);
        agendamento.setDataFim(novaDataFim);
        agendamento.setNomeCliente(nomeCliente);
        agendamento.setTelefoneCliente(telefoneCliente);

        try {
            return agendamentoRepository.save(agendamento);
        } catch (DataIntegrityViolationException e) {
            log.error("[AgendamentoService] Falha de integridade ao atualizar agendamento: {}", e.getMessage());
            throw new ConflitoHorarioException(
                    "Conflito de reserva ao alterar a data. Por favor, selecione outra data disponível."
            );
        }
    }

    /**
     * Sobrecarga para atualização de data única.
     */
    @Transactional
    public Agendamento atualizar(
            Long id,
            Long novaChacaraId,
            Usuario usuarioLogado,
            LocalDate novaData,
            String nomeCliente,
            String telefoneCliente) {
        return atualizar(id, novaChacaraId, usuarioLogado, novaData, novaData, nomeCliente, telefoneCliente);
    }

    /**
     * Lista os agendamentos confirmados de uma chácara dentro de determinado período.
     */
    @Transactional(readOnly = true)
    public List<Agendamento> listarPorChacaraEPeriodo(
            Long chacaraId,
            LocalDate inicio,
            LocalDate fim) {

        return agendamentoRepository.findConflitosNoPeriodo(
                chacaraId,
                StatusAgendamento.CONFIRMADO,
                inicio,
                fim
        );
    }

    /**
     * Exclui definitivamente um agendamento do sistema.
     */
    @Transactional
    public void excluir(Long agendamentoId, Usuario usuarioLogado) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        boolean isDono = agendamento.getUsuario().getId().equals(usuarioLogado.getId());
        boolean isAdmin = usuarioLogado.getRoles().contains(Role.ROLE_ADMIN);

        if (!isDono && !isAdmin) {
            throw new SecurityException("Você não tem permissão para excluir este agendamento");
        }

        agendamentoRepository.delete(agendamento);
        log.info("[AgendamentoService] Agendamento #{} excluído com sucesso por {}", agendamentoId, usuarioLogado.getEmail());
    }

    /**
     * Cancela um agendamento.
     */
    @Transactional
    public void cancelar(Long agendamentoId, Usuario usuarioLogado) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        boolean isDono = agendamento.getUsuario().getId().equals(usuarioLogado.getId());
        boolean isAdmin = usuarioLogado.getRoles().contains(Role.ROLE_ADMIN);

        if (!isDono && !isAdmin) {
            throw new SecurityException("Você não tem permissão para cancelar este agendamento");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }
}