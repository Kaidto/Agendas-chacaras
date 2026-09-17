package com.portfolio.agendamento.config;

import com.portfolio.agendamento.model.Chacara;
import com.portfolio.agendamento.model.Role;
import com.portfolio.agendamento.model.Usuario;
import com.portfolio.agendamento.repository.ChacaraRepository;
import com.portfolio.agendamento.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ChacaraRepository chacaraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(
            ChacaraRepository chacaraRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {

        this.chacaraRepository = chacaraRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {

        // Limpeza defensiva e migração para suporte a múltiplos dias consecutivos (Airbnb style)
        try {
            jdbcTemplate.execute("ALTER TABLE agendamentos DROP CONSTRAINT IF EXISTS uk_chacara_data");
            jdbcTemplate.execute("ALTER TABLE agendamentos DROP CONSTRAINT IF EXISTS uk_chacara_data_turno");
            jdbcTemplate.execute("ALTER TABLE agendamentos DROP COLUMN IF EXISTS turno");
            jdbcTemplate.execute("ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS data_fim DATE");
            jdbcTemplate.execute("UPDATE agendamentos SET data_fim = data_reserva WHERE data_fim IS NULL AND data_reserva IS NOT NULL");
        } catch (Exception ignored) {
        }

        // Limpeza de descrições adicionais para manter os cards limpos
        try {
            jdbcTemplate.execute("UPDATE chacaras SET descricao = NULL");
        } catch (Exception ignored) {
        }

        criarChacaraSeNaoExistir("São Francisco", "/images/chacara_sao_francisco.png");
        criarChacaraSeNaoExistir("Magnólia", "/images/chacara_magnolia.png");

        String adminEmail = System.getenv().getOrDefault("ADMIN_EMAIL", "admin@chacaras.com");
        String adminPassword = System.getenv().getOrDefault("ADMIN_PASSWORD", "admin123");

        criarUsuarioSeNaoExistir(
                "Administrador",
                adminEmail,
                adminPassword,
                Set.of(Role.ROLE_ADMIN, Role.ROLE_USER)
        );

        criarUsuarioSeNaoExistir(
                "Usuário Teste",
                "usuario@teste.com",
                "user1234",
                Set.of(Role.ROLE_USER)
        );
    }

    private void criarChacaraSeNaoExistir(String nome, String imagemPadrao) {
        chacaraRepository.findByNome(nome).ifPresentOrElse(
            chacara -> {
                chacara.setDescricao(null);
                if (chacara.getImagemUrl() == null) {
                    chacara.setImagemUrl(imagemPadrao);
                }
                chacaraRepository.save(chacara);
            },
            () -> {
                Chacara chacara = new Chacara();
                chacara.setNome(nome);
                chacara.setDescricao(null);
                chacara.setImagemUrl(imagemPadrao);
                chacara.setAtiva(true);
                chacaraRepository.save(chacara);
                log.info("[DataSeeder] Chácara inicial cadastrada: {}", nome);
            }
        );
    }

    private void criarUsuarioSeNaoExistir(
            String nomeCompleto,
            String email,
            String senhaPura,
            Set<Role> roles) {

        if (usuarioRepository.findByEmail(email).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setNomeCompleto(nomeCompleto);
            usuario.setEmail(email);
            usuario.setSenha(passwordEncoder.encode(senhaPura));
            usuario.setAtivo(true);
            usuario.setRoles(roles);

            usuarioRepository.save(usuario);
            log.info("[DataSeeder] Usuário inicial criado: {}", email);
        }
    }
}