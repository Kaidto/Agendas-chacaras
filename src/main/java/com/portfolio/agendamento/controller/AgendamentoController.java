package com.portfolio.agendamento.controller;

import com.portfolio.agendamento.dto.AgendamentoRequestDTO;
import com.portfolio.agendamento.dto.AgendamentoResponseDTO;
import com.portfolio.agendamento.dto.AgendamentoUpdateDTO;
import com.portfolio.agendamento.model.Agendamento;
import com.portfolio.agendamento.model.Usuario;
import com.portfolio.agendamento.repository.UsuarioRepository;
import com.portfolio.agendamento.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final UsuarioRepository usuarioRepository;

    public AgendamentoController(
            AgendamentoService agendamentoService,
            UsuarioRepository usuarioRepository) {

        this.agendamentoService = agendamentoService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoResponseDTO>> listar(
            @RequestParam Long chacaraId,
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim) {

        List<Agendamento> lista =
                agendamentoService.listarPorChacaraEPeriodo(
                        chacaraId,
                        inicio,
                        fim
                );

        List<AgendamentoResponseDTO> dtos =
                lista.stream()
                        .map(AgendamentoResponseDTO::fromEntity)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> criar(
            @Valid @RequestBody AgendamentoRequestDTO request,
            Authentication authentication) {

        Usuario usuario =
                usuarioRepository.findByEmail(authentication.getName())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Usuário autenticado não encontrado"
                                ));

        LocalDate dataInicio = request.resolverDataInicio();
        LocalDate dataFim = request.resolverDataFim();

        Agendamento novo =
                agendamentoService.agendar(
                        request.getChacaraId(),
                        usuario,
                        dataInicio,
                        dataFim,
                        request.getNomeCliente(),
                        request.getTelefoneCliente()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AgendamentoResponseDTO.fromEntity(novo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendamentoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AgendamentoUpdateDTO request,
            Authentication authentication) {

        Usuario usuario =
                usuarioRepository.findByEmail(authentication.getName())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Usuário autenticado não encontrado"
                                ));

        LocalDate dataInicio = request.resolverDataInicio();
        LocalDate dataFim = request.resolverDataFim();

        Agendamento atualizado =
                agendamentoService.atualizar(
                        id,
                        request.getChacaraId(),
                        usuario,
                        dataInicio,
                        dataFim,
                        request.getNomeCliente(),
                        request.getTelefoneCliente()
                );

        return ResponseEntity.ok(AgendamentoResponseDTO.fromEntity(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            Authentication authentication) {

        Usuario usuario =
                usuarioRepository.findByEmail(authentication.getName())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Usuário autenticado não encontrado"
                                ));

        agendamentoService.excluir(id, usuario);

        return ResponseEntity.noContent().build();
    }
}