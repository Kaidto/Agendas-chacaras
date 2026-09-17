package com.portfolio.agendamento.controller;

import com.portfolio.agendamento.dto.RegistroUsuarioDTO;
import com.portfolio.agendamento.model.Usuario;
import com.portfolio.agendamento.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody RegistroUsuarioDTO dto) {
        Usuario usuario = usuarioService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "mensagem", "Cadastro realizado com sucesso!",
                "email", usuario.getEmail()
            ));
    }

    // Usado pelo frontend para saber quem esta logado no momento
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of(
            "email", authentication.getName(),
            "autoridades", authentication.getAuthorities()
        ));
    }
}
