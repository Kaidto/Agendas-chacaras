package com.portfolio.agendamento;

import com.portfolio.agendamento.exception.ConflitoHorarioException;
import com.portfolio.agendamento.model.Agendamento;
import com.portfolio.agendamento.model.Chacara;
import com.portfolio.agendamento.model.Role;
import com.portfolio.agendamento.model.Usuario;
import com.portfolio.agendamento.repository.ChacaraRepository;
import com.portfolio.agendamento.repository.UsuarioRepository;
import com.portfolio.agendamento.service.AgendamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa as regras de negocio do sistema de agendamento por data (sem turnos):
 * - Nao e possivel reservar a mesma chacara na mesma data duas vezes.
 * - E possivel reservar chacaras diferentes na mesma data.
 * - E possivel reservar a mesma chacara em datas diferentes.
 * - Nao e permitido agendar em data retroativa.
 */
@SpringBootTest
@Transactional
class AgendamentoServiceTest {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private ChacaraRepository chacaraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Chacara chacaraTeste1;
    private Chacara chacaraTeste2;
    private Usuario usuario1;
    private Usuario usuario2;

    @BeforeEach
    void setUp() {
        chacaraTeste1 = new Chacara();
        chacaraTeste1.setNome("Chacara de Teste 1 " + System.nanoTime());
        chacaraTeste1.setDescricao("Usada apenas nos testes automatizados");
        chacaraTeste1.setAtiva(true);
        chacaraTeste1 = chacaraRepository.save(chacaraTeste1);

        chacaraTeste2 = new Chacara();
        chacaraTeste2.setNome("Chacara de Teste 2 " + System.nanoTime());
        chacaraTeste2.setDescricao("Segunda chacara para testes de concorrencia");
        chacaraTeste2.setAtiva(true);
        chacaraTeste2 = chacaraRepository.save(chacaraTeste2);

        usuario1 = criarUsuario("usuario1_" + System.nanoTime() + "@teste.com");
        usuario2 = criarUsuario("usuario2_" + System.nanoTime() + "@teste.com");
    }

    private Usuario criarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto("Usuario de Teste");
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode("senha12345"));
        usuario.setAtivo(true);
        usuario.setRoles(Set.of(Role.ROLE_USER));
        return usuarioRepository.save(usuario);
    }

    @Test
    void devePermitirAgendarQuandoDataEstaLivre() {
        LocalDate data = LocalDate.now().plusDays(10);

        Agendamento agendamento = agendamentoService.agendar(
            chacaraTeste1.getId(), usuario1, data, "Maria Santos", "(11) 98765-4321"
        );

        assertNotNull(agendamento.getId());
        assertEquals(Agendamento.StatusAgendamento.CONFIRMADO, agendamento.getStatus());
        assertEquals(data, agendamento.getDataReserva());
        assertEquals("Maria Santos", agendamento.getNomeCliente());
    }

    @Test
    void deveImpedirDoisAgendamentosNaMesmaDataParaMesmaChacara() {
        LocalDate data = LocalDate.now().plusDays(15);

        // Usuario 1 agenda com sucesso para o dia
        agendamentoService.agendar(
            chacaraTeste1.getId(), usuario1, data, "Cliente Um", "(11) 91111-1111"
        );

        // Usuario 2 tenta agendar a MESMA chacara no MESMO dia -- deve ser barrado
        ConflitoHorarioException excecao = assertThrows(
            ConflitoHorarioException.class,
            () -> agendamentoService.agendar(
                chacaraTeste1.getId(), usuario2, data, "Cliente Dois", "(11) 92222-2222"
            )
        );

        assertTrue(excecao.getMessage().toLowerCase().contains("reserva") || excecao.getMessage().toLowerCase().contains("reservad"));
    }

    @Test
    void devePermitirMesmaDataParaChacarasDiferentes() {
        LocalDate data = LocalDate.now().plusDays(20);

        Agendamento agendamentoChacara1 = agendamentoService.agendar(
            chacaraTeste1.getId(), usuario1, data, "Cliente Chacara 1", "(11) 93333-3333"
        );

        // Mesma data, porem em outra chacara -- deve permitir
        Agendamento agendamentoChacara2 = agendamentoService.agendar(
            chacaraTeste2.getId(), usuario2, data, "Cliente Chacara 2", "(11) 94444-4444"
        );

        assertNotNull(agendamentoChacara1.getId());
        assertNotNull(agendamentoChacara2.getId());
        assertNotEquals(agendamentoChacara1.getChacara().getId(), agendamentoChacara2.getChacara().getId());
    }

    @Test
    void deveImpedirAgendamentoEmDataPassada() {
        LocalDate ontem = LocalDate.now().minusDays(1);

        assertThrows(
            IllegalArgumentException.class,
            () -> agendamentoService.agendar(
                chacaraTeste1.getId(), usuario1, ontem, "Cliente Passado", "(11) 95555-5555"
            )
        );
    }

    @Test
    void devePermitirAtualizarAgendamento() {
        LocalDate data = LocalDate.now().plusDays(5);
        Agendamento agendamento = agendamentoService.agendar(
            chacaraTeste1.getId(), usuario1, data, "Cliente Original", "(11) 91111-1111"
        );

        LocalDate novaData = LocalDate.now().plusDays(6);
        Agendamento atualizado = agendamentoService.atualizar(
            agendamento.getId(), chacaraTeste1.getId(), usuario1, novaData, "Cliente Modificado", "(11) 99999-8888"
        );

        assertEquals(novaData, atualizado.getDataReserva());
        assertEquals("Cliente Modificado", atualizado.getNomeCliente());
        assertEquals("(11) 99999-8888", atualizado.getTelefoneCliente());
    }

    @Test
    void devePermitirExcluirAgendamentoELiberarData() {
        LocalDate data = LocalDate.now().plusDays(7);
        Agendamento agendamento = agendamentoService.agendar(
            chacaraTeste1.getId(), usuario1, data, "Cliente Para Excluir", "(11) 91111-1111"
        );

        // Exclui o agendamento
        agendamentoService.excluir(agendamento.getId(), usuario1);

        // A mesma data agora deve estar livre para novo agendamento
        Agendamento novoAgendamento = agendamentoService.agendar(
            chacaraTeste1.getId(), usuario2, data, "Outro Cliente", "(11) 92222-2222"
        );
        assertNotNull(novoAgendamento.getId());
        assertEquals(data, novoAgendamento.getDataReserva());
    }

    @Test
    void devePermitirAgendamentoDeMultiplosDiasConsecutivos() {
        LocalDate inicio = LocalDate.now().plusDays(30);
        LocalDate fim = LocalDate.now().plusDays(32); // 3 diárias: 30, 31, 32

        Agendamento agendamento = agendamentoService.agendar(
            chacaraTeste1.getId(), usuario1, inicio, fim, "Cliente Fim de Semana", "(11) 99887-7665"
        );

        assertNotNull(agendamento.getId());
        assertEquals(inicio, agendamento.getDataReserva());
        assertEquals(fim, agendamento.getDataFim());
    }

    @Test
    void deveImpedirConflitoEmQualquerDiaDoPeriodoConsecutivo() {
        LocalDate inicio = LocalDate.now().plusDays(40);
        LocalDate fim = LocalDate.now().plusDays(42); // 40 a 42 ocupados

        agendamentoService.agendar(
            chacaraTeste1.getId(), usuario1, inicio, fim, "Primeiro Cliente", "(11) 91111-2222"
        );

        // Tenta reservar dia 41 (no meio do período) -> Deve falhar
        assertThrows(
            ConflitoHorarioException.class,
            () -> agendamentoService.agendar(
                chacaraTeste1.getId(), usuario2, inicio.plusDays(1), "Cliente Intermediario", "(11) 93333-4444"
            )
        );

        // Tenta reservar com sobreposição inicial (39 a 40) -> Deve falhar
        assertThrows(
            ConflitoHorarioException.class,
            () -> agendamentoService.agendar(
                chacaraTeste1.getId(), usuario2, inicio.minusDays(1), inicio, "Cliente Sobreposicao", "(11) 94444-5555"
            )
        );

        // Reserva dia seguinte livre (dia 43) -> Deve ter sucesso
        Agendamento agendamentoLivre = agendamentoService.agendar(
            chacaraTeste1.getId(), usuario2, fim.plusDays(1), "Cliente Posterior", "(11) 95555-6666"
        );
        assertNotNull(agendamentoLivre.getId());
    }
}
