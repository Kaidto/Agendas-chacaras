/**
 * Lógica da Página de Acesso / Login / Cadastro
 * Com efeito de cursor Antigravity e feedback visual refinado.
 */

const tabLogin = document.getElementById('tabLogin');
const tabCadastro = document.getElementById('tabCadastro');
const formLogin = document.getElementById('formLogin');
const formCadastro = document.getElementById('formCadastro');
const alertBox = document.getElementById('alertBox');
const btnCriarConta = document.getElementById('btnCriarConta');

// -------------------------------------------------------------
// Efeito de Cursor Interativo estilo Antigravity Spotlight
// -------------------------------------------------------------
const cursorGlow = document.getElementById('cursorGlow');
let mouseX = window.innerWidth / 2;
let mouseY = window.innerHeight / 2;
let glowX = mouseX;
let glowY = mouseY;

window.addEventListener('mousemove', (e) => {
  mouseX = e.clientX;
  mouseY = e.clientY;
});

function animarCursor() {
  glowX += (mouseX - glowX) * 0.15;
  glowY += (mouseY - glowY) * 0.15;
  if (cursorGlow) {
    cursorGlow.style.transform = `translate(${glowX - 300}px, ${glowY - 300}px)`;
  }
  requestAnimationFrame(animarCursor);
}
requestAnimationFrame(animarCursor);

// -------------------------------------------------------------
// Gerenciamento de Abas
// -------------------------------------------------------------
function mostrarAba(aba) {
  const ehLogin = aba === 'login';
  formLogin.classList.toggle('hidden', !ehLogin);
  formCadastro.classList.toggle('hidden', ehLogin);

  if (ehLogin) {
    tabLogin.className = 'flex-1 py-2.5 text-xs uppercase font-bold tracking-wider rounded-xl transition-all duration-200 bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 shadow';
    tabCadastro.className = 'flex-1 py-2.5 text-xs uppercase font-bold tracking-wider rounded-xl transition-all duration-200 text-slate-400 hover:text-white';
  } else {
    tabCadastro.className = 'flex-1 py-2.5 text-xs uppercase font-bold tracking-wider rounded-xl transition-all duration-200 bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 shadow';
    tabLogin.className = 'flex-1 py-2.5 text-xs uppercase font-bold tracking-wider rounded-xl transition-all duration-200 text-slate-400 hover:text-white';
  }

  esconderAlerta();
}

function mostrarAlerta(mensagem, tipo = 'erro') {
  alertBox.textContent = mensagem;
  alertBox.className = `mx-6 mt-4 p-3 rounded-xl text-xs font-semibold backdrop-blur-xl border ${
    tipo === 'erro'
      ? 'bg-red-950/80 text-red-200 border-red-500/40'
      : 'bg-emerald-950/80 text-emerald-200 border-emerald-500/40'
  }`;
  alertBox.classList.remove('hidden');
}

function esconderAlerta() {
  alertBox.classList.add('hidden');
}

tabLogin.addEventListener('click', () => mostrarAba('login'));
tabCadastro.addEventListener('click', () => mostrarAba('cadastro'));

// Mensagens vindas de query params (?erro=true / ?logout=true)
const params = new URLSearchParams(window.location.search);
if (params.get('erro')) {
  mostrarAlerta('E-mail ou senha incorretos. Por favor, tente novamente.', 'erro');
}
if (params.get('logout')) {
  mostrarAlerta('Você saiu do sistema com segurança.', 'sucesso');
}

// -------------------------------------------------------------
// Submissão do Cadastro de Novo Usuário
// -------------------------------------------------------------
formCadastro.addEventListener('submit', async (e) => {
  e.preventDefault();
  esconderAlerta();

  const nomeCompleto = document.getElementById('nomeCompleto').value.trim();
  const email = document.getElementById('emailCadastro').value.trim();
  const senha = document.getElementById('senhaCadastro').value;

  if (nomeCompleto.length < 3) {
    mostrarAlerta('O nome deve conter pelo menos 3 caracteres.', 'erro');
    return;
  }

  if (senha.length < 8) {
    mostrarAlerta('A senha precisa ter pelo menos 8 caracteres.', 'erro');
    return;
  }

  if (btnCriarConta) {
    btnCriarConta.disabled = true;
    btnCriarConta.innerHTML = 'Criando conta...';
  }

  try {
    const resp = await fetch('/api/auth/registrar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nomeCompleto, email, senha })
    });

    const data = await resp.json();

    if (!resp.ok) {
      const msg = typeof data === 'object' ? (data.erro || Object.values(data)[0]) : 'Não foi possível concluir o cadastro.';
      mostrarAlerta(msg || 'Não foi possível concluir o cadastro.', 'erro');
      return;
    }

    mostrarAlerta('Conta criada com sucesso! Faça login abaixo.', 'sucesso');
    mostrarAba('login');
    formCadastro.reset();
  } catch (err) {
    mostrarAlerta('Erro de conexão ao comunicar com o servidor.', 'erro');
  } finally {
    if (btnCriarConta) {
      btnCriarConta.disabled = false;
      btnCriarConta.innerHTML = `
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
        </svg>
        <span>Criar Minha Conta</span>
      `;
    }
  }
});
