/**
 * Sistema de Agendamento de Chácaras — Frontend Interativo & Clean
 * Suporta reserva de múltiplos dias consecutivos estilo Airbnb (Check-in e Check-out),
 * fotos personalizadas com download PNG, alteração e exclusão de reservas.
 */

let chacaraAtualId = null;
let nomeChacaraAtual = '';
let todasChacaras = [];
let usuarioLogado = null;
let agendamentosAtuais = [];
let mesVisualizado = new Date();

// Estado de Seleção de Datas Estilo Airbnb
let dataInicioSelecionada = null; // YYYY-MM-DD (Check-in)
let dataFimSelecionada = null;    // YYYY-MM-DD (Check-out)
let dataHoverPreview = null;      // YYYY-MM-DD (Hover entre datas)

const POLLING_INTERVAL_MS = 5000;

// Elementos da interface
const botoesContainer = document.getElementById('chacaraButtons');
const calendarGrid = document.getElementById('calendarGrid');
const mesAnoTitulo = document.getElementById('mesAnoTitulo');
const dataSelecionadaTexto = document.getElementById('dataSelecionadaTexto');
const btnLimparSelecaoDatas = document.getElementById('btnLimparSelecaoDatas');
const inputDataInicioReserva = document.getElementById('dataInicioReserva');
const inputDataFimReserva = document.getElementById('dataFimReserva');
const inputNomeCliente = document.getElementById('nomeCliente');
const inputTelefoneCliente = document.getElementById('telefoneCliente');
const btnAgendar = document.getElementById('btnAgendar');
const btnAgendarTexto = document.getElementById('btnAgendarTexto');
const listaAgendamentos = document.getElementById('listaAgendamentos');
const contadorReservas = document.getElementById('contadorReservas');
const statusAtualizacao = document.getElementById('statusAtualizacao');
const nomeChacaraReserva = document.getElementById('nomeChacaraReserva');
const nomeChacaraLista = document.getElementById('nomeChacaraLista');
const bannerFotoChacara = document.getElementById('bannerFotoChacara');
const linkDownloadBannerPng = document.getElementById('linkDownloadBannerPng');

// Elementos do Modal de Edição
const modalEditar = document.getElementById('modalEditar');
const btnFecharModalEditar = document.getElementById('btnFecharModalEditar');
const editAgendamentoId = document.getElementById('editAgendamentoId');
const editChacaraId = document.getElementById('editChacaraId');
const editDataInicio = document.getElementById('editDataInicio');
const editDataFim = document.getElementById('editDataFim');
const editNomeCliente = document.getElementById('editNomeCliente');
const editTelefoneCliente = document.getElementById('editTelefoneCliente');
const btnSalvarEdicao = document.getElementById('btnSalvarEdicao');
const btnExcluirDoModal = document.getElementById('btnExcluirDoModal');

// Mapa de Fotos Padrão de cada Chácara
const FOTOS_CHACARAS = {
  'sao francisco': {
    png: '/images/chacara_sao_francisco.png',
    nomeArquivo: 'chacara_sao_francisco.png'
  },
  'magnolia': {
    png: '/images/chacara_magnolia.png',
    nomeArquivo: 'chacara_magnolia.png'
  }
};

function obterInfoChacara(chacaraOuNome) {
  let nome = '';
  let imagemCustomizada = null;

  if (typeof chacaraOuNome === 'object' && chacaraOuNome !== null) {
    nome = chacaraOuNome.nome || '';
    imagemCustomizada = chacaraOuNome.imagemUrl;
  } else {
    nome = String(chacaraOuNome || '');
  }

  const chave = (nome || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  const infoBase = chave.includes('magnolia')
    ? { png: '/images/chacara_magnolia.png', nomeArquivo: 'chacara_magnolia.png' }
    : { png: '/images/chacara_sao_francisco.png', nomeArquivo: 'chacara_sao_francisco.png' };

  if (imagemCustomizada) {
    return {
      png: imagemCustomizada,
      nomeArquivo: infoBase.nomeArquivo
    };
  }

  return infoBase;
}

// -------------------------------------------------------------
// Efeito de Cursor Interativo Spotlight Suave
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
    cursorGlow.style.transform = `translate(${glowX - 250}px, ${glowY - 250}px)`;
  }
  requestAnimationFrame(animarCursor);
}
requestAnimationFrame(animarCursor);

// -------------------------------------------------------------
// Formatações e Utilitários
// -------------------------------------------------------------
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function formatarDataBR(dataStr) {
  if (!dataStr) return '';
  const [ano, mes, dia] = dataStr.split('-');
  return `${dia}/${mes}/${ano}`;
}

function calcularDiferencaDias(inicioStr, fimStr) {
  if (!inicioStr || !fimStr) return 0;
  const d1 = new Date(inicioStr + 'T00:00:00');
  const d2 = new Date(fimStr + 'T00:00:00');
  const diffTime = d2.getTime() - d1.getTime();
  return Math.max(0, Math.round(diffTime / (1000 * 60 * 60 * 24)));
}

function obterDataHojeStr() {
  const hoje = new Date();
  const ano = hoje.getFullYear();
  const mes = String(hoje.getMonth() + 1).padStart(2, '0');
  const dia = String(hoje.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

// Máscara de telefone
function aplicarMascaraTelefone(input) {
  if (!input) return;
  input.addEventListener('input', (e) => {
    let val = e.target.value.replace(/\D/g, '');
    if (val.length > 11) val = val.substring(0, 11);
    
    if (val.length > 10) {
      e.target.value = val.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3');
    } else if (val.length > 6) {
      e.target.value = val.replace(/^(\d{2})(\d{4})(\d{0,4})$/, '($1) $2-$3');
    } else if (val.length > 2) {
      e.target.value = val.replace(/^(\d{2})(\d{0,5})$/, '($1) $2');
    } else {
      e.target.value = val;
    }
  });
}
aplicarMascaraTelefone(inputTelefoneCliente);
aplicarMascaraTelefone(editTelefoneCliente);

// Toast Notifications
function exibirToast(mensagem, tipo = 'sucesso') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  const isErro = tipo === 'erro';

  toast.className = `toast-animate pointer-events-auto flex items-center gap-2.5 px-4 py-3 rounded-xl shadow-lg text-xs font-bold border ${
    isErro
      ? 'bg-rose-50 border-rose-200 text-rose-800'
      : 'bg-emerald-50 border-emerald-200 text-emerald-800'
  }`;

  const icone = isErro
    ? `<svg class="w-4 h-4 text-rose-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>`
    : `<svg class="w-4 h-4 text-emerald-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>`;

  toast.innerHTML = `${icone}<span>${escapeHtml(mensagem)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.transition = 'all 0.3s ease';
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// -------------------------------------------------------------
// Carregamento do Usuário Logado
// -------------------------------------------------------------
async function carregarUsuarioLogado() {
  try {
    const resp = await fetch('/api/auth/me');
    if (resp.ok) {
      usuarioLogado = await resp.json();
      const elNome = document.getElementById('usuarioLogadoNome');
      const elEmail = document.getElementById('usuarioLogadoEmail');
      
      const isAdmin = usuarioLogado.autoridades && usuarioLogado.autoridades.some(a => a.authority === 'ROLE_ADMIN');
      if (elNome) elNome.textContent = usuarioLogado.email.split('@')[0];
      if (elEmail) elEmail.textContent = isAdmin ? 'Administrador' : usuarioLogado.email;
    } else if (resp.status === 401) {
      window.location.href = '/login.html';
    }
  } catch (err) {
    console.error('Falha ao autenticar usuário:', err);
  }
}

// -------------------------------------------------------------
// Carregamento e Seleção de Chácaras
// -------------------------------------------------------------
async function carregarChacaras() {
  try {
    const resp = await fetch('/api/chacaras');
    if (!resp.ok) throw new Error('Falha ao consultar chácaras');

    todasChacaras = await resp.json();
    if (!todasChacaras || todasChacaras.length === 0) {
      botoesContainer.innerHTML = '<p class="text-sm text-slate-500">Nenhuma chácara ativa encontrada.</p>';
      return;
    }

    if (editChacaraId) {
      editChacaraId.innerHTML = todasChacaras.map(c => `
        <option value="${c.id}">${escapeHtml(c.nome)}</option>
      `).join('');
    }

    botoesContainer.innerHTML = todasChacaras.map((c, i) => {
      const info = obterInfoChacara(c);

      return `
        <div
          data-id="${c.id}"
          data-nome="${escapeHtml(c.nome)}"
          class="chacara-card relative p-3.5 sm:p-4 rounded-2xl bg-white border transition-all duration-200 cursor-pointer overflow-hidden shadow-sm ${
            i === 0
              ? 'border-emerald-500 bg-emerald-50/40 ring-2 ring-emerald-500/20'
              : 'border-slate-200 hover:border-slate-300'
          }">
          <div class="flex items-center gap-3 sm:gap-4">
            
            <!-- Foto da Chácara -->
            <div class="relative w-24 h-20 sm:w-28 sm:h-20 rounded-xl overflow-hidden shrink-0 border border-slate-200 group shadow-sm">
              <img id="fotoChacaraThumb_${c.id}" src="${info.png}" alt="${escapeHtml(c.nome)}" class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105">
            </div>

            <!-- Identificação e Botões -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between gap-1 mb-2.5">
                <h3 class="font-extrabold text-sm sm:text-base text-slate-900 truncate">${escapeHtml(c.nome)}</h3>
                <span class="badge-status text-[10px] uppercase font-bold tracking-wider px-2.5 py-0.5 rounded-full shrink-0 ${
                  i === 0
                    ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                    : 'bg-slate-100 text-slate-500 border border-slate-200'
                }">
                  ${i === 0 ? 'Selecionada' : 'Escolher'}
                </span>
              </div>

              <!-- Botões de Ação para a Foto -->
              <div class="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onclick="event.stopPropagation(); dispararUploadFoto(${c.id});"
                  class="bg-slate-50 hover:bg-emerald-50 text-slate-700 hover:text-emerald-800 border border-slate-200 hover:border-emerald-300 px-2.5 py-1 rounded-lg text-[11px] font-bold flex items-center gap-1.5 transition cursor-pointer shadow-none hover:shadow-sm"
                  title="Alterar a foto desta chácara (PNG)">
                  <svg class="w-3.5 h-3.5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12" />
                  </svg>
                  <span>Alterar PNG</span>
                </button>

                <a
                  id="linkDownloadCardPng_${c.id}"
                  href="${info.png}"
                  download="${info.nomeArquivo}"
                  onclick="event.stopPropagation();"
                  class="bg-slate-50 hover:bg-emerald-50 text-slate-700 hover:text-emerald-800 border border-slate-200 hover:border-emerald-300 px-2.5 py-1 rounded-lg text-[11px] font-bold flex items-center gap-1.5 transition cursor-pointer shadow-none hover:shadow-sm"
                  title="Baixar imagem atual em formato PNG">
                  <svg class="w-3.5 h-3.5 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                  </svg>
                  <span>Baixar PNG</span>
                </a>
              </div>
            </div>

          </div>
        </div>
      `;
    }).join('');

    // Listener nos cards de chácara
    document.querySelectorAll('.chacara-card').forEach(btn => {
      btn.addEventListener('click', () => {
        selecionarChacara(Number(btn.dataset.id), btn.dataset.nome, btn);
      });
    });

    if (todasChacaras.length > 0) {
      selecionarChacara(todasChacaras[0].id, todasChacaras[0].nome, document.querySelector('.chacara-card'));
    }

  } catch (err) {
    botoesContainer.innerHTML = '<p class="text-xs text-rose-500 font-semibold">Não foi possível carregar as chácaras disponíveis.</p>';
  }
}

function selecionarChacara(id, nome, elementoCard) {
  chacaraAtualId = id;
  nomeChacaraAtual = nome;

  if (nomeChacaraReserva) nomeChacaraReserva.textContent = nome;
  if (nomeChacaraLista) nomeChacaraLista.textContent = nome;

  // Atualizar Banner Fotográfico no Card de Reserva
  const chacaraObj = todasChacaras.find(c => c.id === id);
  const info = obterInfoChacara(chacaraObj || nome);
  if (bannerFotoChacara) {
    bannerFotoChacara.src = info.png;
  }
  if (linkDownloadBannerPng) {
    linkDownloadBannerPng.href = info.png;
    linkDownloadBannerPng.setAttribute('download', info.nomeArquivo);
  }

  document.querySelectorAll('.chacara-card').forEach(card => {
    card.classList.remove('border-emerald-500', 'bg-emerald-50/40', 'ring-2', 'ring-emerald-500/20');
    card.classList.add('border-slate-200');
    const badge = card.querySelector('.badge-status');
    if (badge) {
      badge.textContent = 'Escolher';
      badge.className = 'badge-status text-[10px] uppercase font-bold tracking-wider px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-500 border border-slate-200';
    }
  });

  if (elementoCard) {
    elementoCard.classList.remove('border-slate-200');
    elementoCard.classList.add('border-emerald-500', 'bg-emerald-50/40', 'ring-2', 'ring-emerald-500/20');
    const badge = elementoCard.querySelector('.badge-status');
    if (badge) {
      badge.textContent = 'Selecionada';
      badge.className = 'badge-status text-[10px] uppercase font-bold tracking-wider px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-300';
    }
  }

  limparSelecaoDatas();
  carregarAgendamentos();
}

// -------------------------------------------------------------
// Carregar Agendamentos da Chácara Selecionada
// -------------------------------------------------------------
async function carregarAgendamentos() {
  if (!chacaraAtualId) return;

  const hoje = new Date();
  const inicio = new Date(hoje.getFullYear(), hoje.getMonth() - 1, 1);
  const fim = new Date(hoje.getFullYear(), hoje.getMonth() + 6, 0);

  const inicioStr = inicio.toISOString().split('T')[0];
  const fimStr = fim.toISOString().split('T')[0];

  try {
    const resp = await fetch(`/api/agendamentos?chacaraId=${chacaraAtualId}&inicio=${inicioStr}&fim=${fimStr}`);
    if (!resp.ok) return;

    agendamentosAtuais = await resp.json();

    renderizarCalendario();
    renderizarLista();

    if (statusAtualizacao) {
      statusAtualizacao.textContent = `Atualizado às ${new Date().toLocaleTimeString('pt-BR')}`;
    }
  } catch (err) {
    if (statusAtualizacao) statusAtualizacao.textContent = 'Tentando sincronizar...';
  }
}

// -------------------------------------------------------------
// Verifica se um determinado dia está ocupado
// -------------------------------------------------------------
function obterReservaDoDia(dataStr) {
  return agendamentosAtuais.find(ag => {
    const inicio = ag.dataInicio || ag.dataReserva;
    const fim = ag.dataFim || ag.dataReserva;
    return inicio <= dataStr && fim >= dataStr;
  });
}

function periodoTemConflito(inicioStr, fimStr) {
  return agendamentosAtuais.some(ag => {
    const agInicio = ag.dataInicio || ag.dataReserva;
    const agFim = ag.dataFim || ag.dataReserva;
    return agInicio <= fimStr && agFim >= inicioStr;
  });
}

// -------------------------------------------------------------
// Renderização do Calendário Interativo Estilo Airbnb
// -------------------------------------------------------------
function renderizarCalendario() {
  if (!calendarGrid) return;

  const ano = mesVisualizado.getFullYear();
  const mes = mesVisualizado.getMonth();

  const nomesMeses = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];

  if (mesAnoTitulo) {
    mesAnoTitulo.textContent = `${nomesMeses[mes]} de ${ano}`;
  }

  const primeiroDiaSemana = new Date(ano, mes, 1).getDay();
  const totalDiasMes = new Date(ano, mes + 1, 0).getDate();
  const hojeStr = obterDataHojeStr();

  let html = '';

  for (let i = 0; i < primeiroDiaSemana; i++) {
    html += `<div class="calendar-day empty"></div>`;
  }

  for (let dia = 1; dia <= totalDiasMes; dia++) {
    const diaFormatado = String(dia).padStart(2, '0');
    const mesFormatado = String(mes + 1).padStart(2, '0');
    const dataStr = `${ano}-${mesFormatado}-${diaFormatado}`;

    const isPassado = dataStr < hojeStr;
    const isHoje = dataStr === hojeStr;
    const reservaExistente = obterReservaDoDia(dataStr);
    const isReservado = !!reservaExistente;

    let classes = ['calendar-day'];

    if (isPassado) {
      classes.push('past');
    } else if (isReservado) {
      classes.push('reserved');
    } else {
      classes.push('available');
    }

    if (isHoje) classes.push('today');

    // Lógica de seleção estilo Airbnb
    if (dataInicioSelecionada && dataFimSelecionada) {
      if (dataInicioSelecionada === dataFimSelecionada && dataStr === dataInicioSelecionada) {
        classes.push('selected-single');
      } else if (dataStr === dataInicioSelecionada) {
        classes.push('range-start');
      } else if (dataStr === dataFimSelecionada) {
        classes.push('range-end');
      } else if (dataStr > dataInicioSelecionada && dataStr < dataFimSelecionada) {
        classes.push('range-between');
      }
    } else if (dataInicioSelecionada && !dataFimSelecionada) {
      if (dataStr === dataInicioSelecionada) {
        classes.push('selected-single');
      } else if (dataHoverPreview && dataStr > dataInicioSelecionada && dataStr <= dataHoverPreview && !isReservado) {
        classes.push('range-preview');
      }
    }

    html += `
      <div
        class="${classes.join(' ')}"
        data-date="${dataStr}"
        data-reserved="${isReservado ? 'true' : 'false'}"
        data-past="${isPassado ? 'true' : 'false'}"
        title="${
          isPassado
            ? 'Data passada'
            : isReservado
            ? `Reservado por ${escapeHtml(reservaExistente.nomeCliente)}`
            : isHoje
            ? 'Hoje — Disponível para agendamento'
            : 'Disponível — Clique para agendar'
        }">
        <span class="z-10">${dia}</span>
      </div>
    `;
  }

  calendarGrid.innerHTML = html;

  // Listeners de clique e hover
  calendarGrid.querySelectorAll('.calendar-day').forEach(el => {
    el.addEventListener('click', () => {
      const data = el.dataset.date;
      const isPast = el.dataset.past === 'true';
      const isRes = el.dataset.reserved === 'true';

      if (isPast) {
        exibirToast('Não é possível agendar datas retroativas.', 'erro');
        return;
      }

      if (isRes) {
        const ag = obterReservaDoDia(data);
        if (ag) {
          const userEmail = usuarioLogado ? usuarioLogado.email : '';
          const isAdmin = usuarioLogado && usuarioLogado.autoridades && usuarioLogado.autoridades.some(a => a.authority === 'ROLE_ADMIN');
          const isMinhaReserva = (ag.emailUsuario && ag.emailUsuario === userEmail) || isAdmin;

          if (isMinhaReserva) {
            abrirModalEditar(ag);
          } else {
            exibirToast(`A data ${formatarDataBR(data)} já está reservada para ${ag.nomeCliente}.`, 'erro');
          }
        }
        return;
      }

      selecionarData(data);
    });

    el.addEventListener('mouseenter', () => {
      const data = el.dataset.date;
      if (dataInicioSelecionada && !dataFimSelecionada && data > dataInicioSelecionada) {
        dataHoverPreview = data;
        renderizarCalendario();
      }
    });
  });

  calendarGrid.addEventListener('mouseleave', () => {
    if (dataHoverPreview) {
      dataHoverPreview = null;
      renderizarCalendario();
    }
  });
}

// -------------------------------------------------------------
// Lógica de Seleção de Datas Estilo Airbnb
// -------------------------------------------------------------
function selecionarData(dataStr) {
  // Caso 1: Nenhuma data selecionada OU ambas já selecionadas -> Inicia nova seleção
  if (!dataInicioSelecionada || (dataInicioSelecionada && dataFimSelecionada)) {
    dataInicioSelecionada = dataStr;
    dataFimSelecionada = null;
    dataHoverPreview = null;
    atualizarDestaquePeriodo();
    renderizarCalendario();
    return;
  }

  // Caso 2: Data inicial já selecionada -> Definindo data final
  if (dataInicioSelecionada && !dataFimSelecionada) {
    if (dataStr < dataInicioSelecionada) {
      // Se clicou em data anterior, ajusta a data inicial para a nova
      dataInicioSelecionada = dataStr;
      dataFimSelecionada = null;
    } else if (dataStr === dataInicioSelecionada) {
      // Clicou no mesmo dia -> Reserva de apenas 1 diária
      dataFimSelecionada = dataStr;
    } else {
      // Período consecutivo: valida se existe conflito no intervalo
      if (periodoTemConflito(dataInicioSelecionada, dataStr)) {
        exibirToast('O período selecionado contém dias já ocupados. Escolha um período livre.', 'erro');
        dataInicioSelecionada = dataStr;
        dataFimSelecionada = null;
      } else {
        dataFimSelecionada = dataStr;
        const total = calcularDiferencaDias(dataInicioSelecionada, dataFimSelecionada) + 1;
        exibirToast(`Período selecionado: ${total} diárias consecutivas!`, 'sucesso');
      }
    }

    dataHoverPreview = null;
    atualizarDestaquePeriodo();
    renderizarCalendario();

    if (dataInicioSelecionada && dataFimSelecionada && !inputNomeCliente.value.trim()) {
      inputNomeCliente.focus();
    }
  }
}

function limparSelecaoDatas() {
  dataInicioSelecionada = null;
  dataFimSelecionada = null;
  dataHoverPreview = null;
  atualizarDestaquePeriodo();
  renderizarCalendario();
}

if (btnLimparSelecaoDatas) {
  btnLimparSelecaoDatas.addEventListener('click', limparSelecaoDatas);
}

function atualizarDestaquePeriodo() {
  if (inputDataInicioReserva) inputDataInicioReserva.value = dataInicioSelecionada || '';
  if (inputDataFimReserva) inputDataFimReserva.value = dataFimSelecionada || '';

  if (!dataInicioSelecionada) {
    if (dataSelecionadaTexto) dataSelecionadaTexto.textContent = 'Selecione os dias no calendário';
    if (btnLimparSelecaoDatas) btnLimparSelecaoDatas.classList.add('hidden');
    if (btnAgendarTexto) btnAgendarTexto.textContent = 'Confirmar Reserva';
    btnAgendar.disabled = false;
    return;
  }

  if (btnLimparSelecaoDatas) btnLimparSelecaoDatas.classList.remove('hidden');

  if (dataInicioSelecionada && !dataFimSelecionada) {
    if (dataSelecionadaTexto) {
      dataSelecionadaTexto.innerHTML = `Check-in: <strong class="text-emerald-700">${formatarDataBR(dataInicioSelecionada)}</strong> &bull; Escolha o Check-out`;
    }
    if (btnAgendarTexto) btnAgendarTexto.textContent = 'Escolha o Check-out no calendário';
    btnAgendar.disabled = true;
    return;
  }

  if (dataInicioSelecionada && dataFimSelecionada) {
    const totalDias = calcularDiferencaDias(dataInicioSelecionada, dataFimSelecionada) + 1;
    if (dataSelecionadaTexto) {
      if (totalDias === 1) {
        dataSelecionadaTexto.innerHTML = `<strong class="text-emerald-800">${formatarDataBR(dataInicioSelecionada)}</strong> (1 diária)`;
      } else {
        dataSelecionadaTexto.innerHTML = `<strong class="text-emerald-800">${formatarDataBR(dataInicioSelecionada)}</strong> até <strong class="text-emerald-800">${formatarDataBR(dataFimSelecionada)}</strong> (${totalDias} diárias)`;
      }
    }
    btnAgendar.disabled = false;
    if (btnAgendarTexto) {
      btnAgendarTexto.textContent = `Confirmar Reserva (${totalDias} ${totalDias === 1 ? 'diária' : 'diárias'})`;
    }
  }
}

// -------------------------------------------------------------
// Navegação do Mês
// -------------------------------------------------------------
document.getElementById('btnMesAnterior').addEventListener('click', () => {
  mesVisualizado.setMonth(mesVisualizado.getMonth() - 1);
  renderizarCalendario();
});

document.getElementById('btnMesProximo').addEventListener('click', () => {
  mesVisualizado.setMonth(mesVisualizado.getMonth() + 1);
  renderizarCalendario();
});

document.getElementById('btnMesAtual').addEventListener('click', () => {
  mesVisualizado = new Date();
  renderizarCalendario();
});

// -------------------------------------------------------------
// Renderização da Lista de Reservas Confirmadas
// -------------------------------------------------------------
function renderizarLista() {
  if (!listaAgendamentos) return;

  const hojeStr = obterDataHojeStr();
  const futuros = agendamentosAtuais.filter(a => (a.dataFim || a.dataReserva) >= hojeStr);

  futuros.sort((a, b) => (a.dataInicio || a.dataReserva).localeCompare(b.dataInicio || b.dataReserva));

  if (contadorReservas) {
    contadorReservas.textContent = `${futuros.length} ${futuros.length === 1 ? 'reserva' : 'reservas'}`;
  }

  if (futuros.length === 0) {
    listaAgendamentos.innerHTML = `
      <div class="text-center py-6">
        <p class="text-xs text-slate-500">Nenhuma reserva agendada para os próximos meses.</p>
        <p class="text-[11px] text-emerald-700 font-bold mt-1">Todos os dias estão livres!</p>
      </div>
    `;
    return;
  }

  const userEmail = usuarioLogado ? usuarioLogado.email : '';
  const isAdmin = usuarioLogado && usuarioLogado.autoridades && usuarioLogado.autoridades.some(a => a.authority === 'ROLE_ADMIN');

  listaAgendamentos.innerHTML = futuros.map(ag => {
    const isMinhaReserva = (ag.emailUsuario && ag.emailUsuario === userEmail) || isAdmin;
    const inicio = ag.dataInicio || ag.dataReserva;
    const fim = ag.dataFim || ag.dataReserva;
    const dias = calcularDiferencaDias(inicio, fim) + 1;

    const periodoTexto = dias === 1
      ? `${formatarDataBR(inicio)} (1 diária)`
      : `${formatarDataBR(inicio)} a ${formatarDataBR(fim)} (${dias} diárias)`;

    return `
      <div class="p-3 rounded-xl bg-slate-50 hover:bg-slate-100/80 border border-slate-200/90 transition flex items-center justify-between gap-2 shadow-sm">
        <div class="flex items-center gap-3 min-w-0">
          <div class="w-8 h-8 rounded-lg bg-emerald-100 border border-emerald-300/80 flex items-center justify-center text-emerald-800 font-extrabold text-xs shrink-0">
            ${inicio.split('-')[2]}
          </div>
          <div class="min-w-0">
            <div class="flex items-center gap-1.5 flex-wrap">
              <p class="text-xs font-bold text-slate-900">${periodoTexto}</p>
            </div>
            <p class="text-[11px] text-slate-500 truncate">
              Cliente: <strong class="text-slate-800 font-semibold">${escapeHtml(ag.nomeCliente)}</strong>
              ${ag.telefoneCliente ? `<span class="text-slate-400 hidden sm:inline">&bull; ${escapeHtml(ag.telefoneCliente)}</span>` : ''}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-1.5 shrink-0">
          ${
            isMinhaReserva
              ? `
                <!-- Botão Alterar -->
                <button
                  type="button"
                  data-json="${encodeURIComponent(JSON.stringify(ag))}"
                  class="btn-alterar-item text-[11px] font-bold text-emerald-700 hover:text-emerald-900 bg-emerald-50 hover:bg-emerald-100 border border-emerald-300/80 p-1.5 rounded-lg transition flex items-center gap-1 shadow-none hover:shadow-sm cursor-pointer"
                  title="Alterar dados ou período da reserva">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                  </svg>
                  <span class="hidden sm:inline">Editar</span>
                </button>

                <!-- Botão Excluir -->
                <button
                  type="button"
                  data-id="${ag.id}"
                  data-cliente="${escapeHtml(ag.nomeCliente)}"
                  data-periodo="${periodoTexto}"
                  class="btn-excluir-item text-[11px] font-bold text-rose-600 hover:text-rose-800 bg-rose-50 hover:bg-rose-100 border border-rose-200 p-1.5 rounded-lg transition flex items-center gap-1 shadow-none hover:shadow-sm cursor-pointer"
                  title="Excluir reserva e liberar datas">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  <span class="hidden sm:inline">Excluir</span>
                </button>
              `
              : `<span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-500 border border-slate-200">Reservado</span>`
          }
        </div>
      </div>
    `;
  }).join('');

  // Listeners dos botões da lista
  listaAgendamentos.querySelectorAll('.btn-alterar-item').forEach(btn => {
    btn.addEventListener('click', () => {
      const ag = JSON.parse(decodeURIComponent(btn.dataset.json));
      abrirModalEditar(ag);
    });
  });

  listaAgendamentos.querySelectorAll('.btn-excluir-item').forEach(btn => {
    btn.addEventListener('click', () => {
      confirmarExcluirAgendamento(Number(btn.dataset.id), btn.dataset.cliente, btn.dataset.periodo);
    });
  });
}

// -------------------------------------------------------------
// MODAL DE EDIÇÃO DE AGENDAMENTO
// -------------------------------------------------------------
function abrirModalEditar(ag) {
  if (!modalEditar) return;

  editAgendamentoId.value = ag.id;
  editChacaraId.value = ag.chacaraId;
  editDataInicio.value = ag.dataInicio || ag.dataReserva;
  editDataFim.value = ag.dataFim || ag.dataReserva;
  editNomeCliente.value = ag.nomeCliente;
  editTelefoneCliente.value = ag.telefoneCliente || '';

  modalEditar.classList.remove('hidden');
}

function fecharModalEditar() {
  if (modalEditar) {
    modalEditar.classList.add('hidden');
  }
}

if (btnFecharModalEditar) {
  btnFecharModalEditar.addEventListener('click', fecharModalEditar);
}

// Salvar Edição (PUT)
if (btnSalvarEdicao) {
  btnSalvarEdicao.addEventListener('click', async () => {
    const id = editAgendamentoId.value;
    const chacaraId = Number(editChacaraId.value);
    const dataInicio = editDataInicio.value;
    const dataFim = editDataFim.value || dataInicio;
    const nomeCliente = editNomeCliente.value.trim();
    const telefoneCliente = editTelefoneCliente.value.trim();

    if (!dataInicio) {
      exibirToast('Por favor, informe a data de início da reserva.', 'erro');
      return;
    }

    if (dataFim < dataInicio) {
      exibirToast('A data de check-out deve ser posterior ou igual ao check-in.', 'erro');
      return;
    }

    if (!nomeCliente || nomeCliente.length < 2) {
      exibirToast('Informe o nome do cliente responsável.', 'erro');
      return;
    }

    if (!telefoneCliente || telefoneCliente.length < 10) {
      exibirToast('Informe um telefone válido.', 'erro');
      return;
    }

    btnSalvarEdicao.disabled = true;
    btnSalvarEdicao.innerHTML = 'Salvando...';

    try {
      const resp = await fetch(`/api/agendamentos/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          chacaraId,
          dataInicio,
          dataFim,
          nomeCliente,
          telefoneCliente
        })
      });

      const data = await resp.json();

      if (resp.status === 409) {
        exibirToast(data.erro || 'O período selecionado já possui reservas.', 'erro');
      } else if (!resp.ok) {
        const msg = typeof data === 'object' ? (data.erro || Object.values(data)[0]) : 'Falha ao atualizar reserva.';
        exibirToast(msg, 'erro');
      } else {
        exibirToast('Reserva alterada com sucesso!', 'sucesso');
        fecharModalEditar();
        await carregarAgendamentos();
      }
    } catch (err) {
      exibirToast('Erro de conexão ao salvar alterações.', 'erro');
    } finally {
      btnSalvarEdicao.disabled = false;
      btnSalvarEdicao.innerHTML = `
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        <span>Salvar Alterações</span>
      `;
    }
  });
}

// Excluir a partir do modal
if (btnExcluirDoModal) {
  btnExcluirDoModal.addEventListener('click', () => {
    const id = Number(editAgendamentoId.value);
    const cliente = editNomeCliente.value;
    const periodo = `${formatarDataBR(editDataInicio.value)} a ${formatarDataBR(editDataFim.value)}`;
    fecharModalEditar();
    confirmarExcluirAgendamento(id, cliente, periodo);
  });
}

// -------------------------------------------------------------
// Exclusão de Agendamento (DELETE)
// -------------------------------------------------------------
async function confirmarExcluirAgendamento(id, cliente, periodo) {
  if (!confirm(`Deseja realmente EXCLUIR a reserva de ${cliente} (${periodo})? As datas serão liberadas imediatamente no calendário.`)) {
    return;
  }

  try {
    const resp = await fetch(`/api/agendamentos/${id}`, {
      method: 'DELETE'
    });

    if (resp.ok) {
      exibirToast('Reserva excluída com sucesso! Datas liberadas.', 'sucesso');
      await carregarAgendamentos();
    } else {
      exibirToast('Não foi possível excluir o agendamento.', 'erro');
    }
  } catch (err) {
    exibirToast('Erro de conexão ao excluir.', 'erro');
  }
}

// -------------------------------------------------------------
// Criação de Agendamento (POST)
// -------------------------------------------------------------
btnAgendar.addEventListener('click', async () => {
  if (!chacaraAtualId) {
    exibirToast('Selecione uma chácara primeiro.', 'erro');
    return;
  }

  if (!dataInicioSelecionada) {
    exibirToast('Selecione ao menos a data inicial (Check-in) no calendário.', 'erro');
    return;
  }

  const dataFimFinal = dataFimSelecionada || dataInicioSelecionada;
  const nomeCliente = inputNomeCliente.value.trim();
  const telefoneCliente = inputTelefoneCliente.value.trim();

  if (!nomeCliente || nomeCliente.length < 2) {
    exibirToast('Por favor, informe o nome do cliente.', 'erro');
    inputNomeCliente.focus();
    return;
  }

  if (!telefoneCliente || telefoneCliente.length < 10) {
    exibirToast('Por favor, informe um telefone de contato válido.', 'erro');
    inputTelefoneCliente.focus();
    return;
  }

  btnAgendar.disabled = true;
  btnAgendar.innerHTML = `
    <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
    </svg>
    Confirmando reserva...
  `;

  try {
    const resp = await fetch('/api/agendamentos', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        chacaraId: chacaraAtualId,
        dataInicio: dataInicioSelecionada,
        dataFim: dataFimFinal,
        nomeCliente,
        telefoneCliente
      })
    });

    const data = await resp.json();

    if (resp.status === 409) {
      exibirToast(data.erro || 'O período selecionado já possui reservas confirmadas.', 'erro');
      carregarAgendamentos();
    } else if (!resp.ok) {
      const msg = typeof data === 'object' ? (data.erro || Object.values(data)[0]) : 'Não foi possível confirmar a reserva.';
      exibirToast(msg, 'erro');
    } else {
      exibirToast('Reserva confirmada com sucesso!', 'sucesso');
      
      inputNomeCliente.value = '';
      inputTelefoneCliente.value = '';
      limparSelecaoDatas();

      await carregarAgendamentos();
    }
  } catch (err) {
    exibirToast('Erro de comunicação com o servidor. Verifique sua conexão.', 'erro');
  } finally {
    btnAgendar.disabled = false;
    atualizarDestaquePeriodo();
  }
});

// -------------------------------------------------------------
// Upload e Alteração de Fotos das Chácaras (PNG)
// -------------------------------------------------------------
let inputUploadCard = document.getElementById('inputUploadCard');
if (!inputUploadCard) {
  inputUploadCard = document.createElement('input');
  inputUploadCard.type = 'file';
  inputUploadCard.id = 'inputUploadCard';
  inputUploadCard.accept = 'image/png,image/jpeg,image/webp';
  inputUploadCard.className = 'hidden';
  document.body.appendChild(inputUploadCard);
}

let chacaraAlvoUploadId = null;

function dispararUploadFoto(chacaraId) {
  chacaraAlvoUploadId = chacaraId;
  inputUploadCard.click();
}

inputUploadCard.addEventListener('change', async (e) => {
  const file = e.target.files && e.target.files[0];
  if (!file || !chacaraAlvoUploadId) return;
  await enviarFotoChacara(chacaraAlvoUploadId, file);
  inputUploadCard.value = '';
});

const btnAlterarFotoBanner = document.getElementById('btnAlterarFotoBanner');
const inputUploadBanner = document.getElementById('inputUploadBanner');

if (btnAlterarFotoBanner && inputUploadBanner) {
  btnAlterarFotoBanner.addEventListener('click', () => {
    if (!chacaraAtualId) return;
    inputUploadBanner.click();
  });

  inputUploadBanner.addEventListener('change', async (e) => {
    const file = e.target.files && e.target.files[0];
    if (!file || !chacaraAtualId) return;
    await enviarFotoChacara(chacaraAtualId, file);
    inputUploadBanner.value = '';
  });
}

async function enviarFotoChacara(chacaraId, file) {
  try {
    exibirToast('Enviando nova imagem...', 'sucesso');
    const formData = new FormData();
    formData.append('arquivo', file);

    const resp = await fetch(`/api/chacaras/${chacaraId}/imagem`, {
      method: 'POST',
      body: formData
    });

    if (!resp.ok) {
      const erroData = await resp.json().catch(() => ({}));
      throw new Error(erroData.erro || 'Erro ao enviar imagem');
    }

    const resultado = await resp.json();
    const urlAtualizada = `${resultado.imagemUrl}?t=${Date.now()}`;

    // Atualizar objeto em memória
    const ch = todasChacaras.find(c => c.id === chacaraId);
    if (ch) ch.imagemUrl = resultado.imagemUrl;

    // Atualizar miniatura no card da chácara
    const thumb = document.getElementById(`fotoChacaraThumb_${chacaraId}`);
    if (thumb) thumb.src = urlAtualizada;

    const linkCard = document.getElementById(`linkDownloadCardPng_${chacaraId}`);
    if (linkCard) linkCard.href = urlAtualizada;

    // Se a chácara alterada for a selecionada atualmente, atualiza o banner
    if (chacaraAtualId === chacaraId) {
      if (bannerFotoChacara) bannerFotoChacara.src = urlAtualizada;
      if (linkDownloadBannerPng) linkDownloadBannerPng.href = urlAtualizada;
    }

    exibirToast('Foto da chácara atualizada com sucesso!', 'sucesso');
  } catch (err) {
    console.error('Erro no upload de foto:', err);
    exibirToast(err.message || 'Falha ao atualizar foto', 'erro');
  }
}

// -------------------------------------------------------------
// Inicialização
// -------------------------------------------------------------
carregarUsuarioLogado();
carregarChacaras();

setInterval(carregarAgendamentos, POLLING_INTERVAL_MS);