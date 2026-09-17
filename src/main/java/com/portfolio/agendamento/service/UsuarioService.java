package com.portfolio.agendamento.service;

import com.portfolio.agendamento.dto.RegistroUsuarioDTO;
import com.portfolio.agendamento.model.Role;
import com.portfolio.agendamento.model.Usuario;
import com.portfolio.agendamento.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario registrar(RegistroUsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail().toLowerCase().trim())) {
            throw new IllegalArgumentException("Ja existe uma conta cadastrada com este e-mail");
        }

        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(dto.getNomeCompleto().trim());
        usuario.setEmail(dto.getEmail().toLowerCase().trim());
        // Nunca armazenar a senha em texto plano -- hash BCrypt antes de persistir
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setAtivo(true);
        usuario.setRoles(Set.of(Role.ROLE_USER));

        return usuarioRepository.save(usuario);
    }
}
