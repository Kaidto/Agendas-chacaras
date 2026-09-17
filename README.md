# Sistema de Agendamento — Chácaras São Francisco & Magnólia

Sistema web moderno, exclusivo e de alta performance para reserva de diárias de chácaras de lazer (São Francisco & Magnólia). Desenvolvido com arquitetura corporativa Spring Boot 3, segurança de ponta, estética limpa e refinada (*clean theme*), calendário interativo no estilo Airbnb para reservas de dias consecutivos e pronto para deploy no **Render** e **GitHub**.

---

## ✨ Funcionalidades e Diferenciais

1. **Reservas de Múltiplos Dias Consecutivos (Estilo Airbnb):**
   - Seleção intuitiva de período: **1º clique** define o *Check-in*, **2º clique** define o *Check-out*.
   - Efeito visual de hover destacando o intervalo selecionado (*range preview*).
   - Cálculo e exibição automática da quantidade de diárias e do período escolhido (ex: `18/09/2026 até 20/09/2026 (2 diárias)`).
   - Validação matemática de sobreposição de intervalos no backend (`interval overlap query`), impedindo qualquer conflito de datas.

2. **Design Clean & Contemporâneo:**
   - Visual claro, arejado e elegante inspirado em resorts e plataformas modernas.
   - Tipografia refinada (*Plus Jakarta Sans*), cards brancos com bordas suaves, sombras sutis e acentos em verde esmeralda.
   - **Sticky Action Card:** o formulário de reserva e o botão de confirmação permanecem sempre visíveis na tela durante a navegação.
   - Efeito spotlight interativo e notificações *Toast* em tempo real.

3. **Gerenciamento Completo de Reservas:**
   - Visualização das reservas com status, período e total de diárias.
   - **Alteração de Reserva:** modal integrado para mudar as datas ou observações.
   - **Cancelamento/Exclusão:** remoção de agendamentos com liberação instantânea das datas no calendário.

4. **Upload Personalizado de Fotos em PNG:**
   - Botão para envio de imagem própria (PNG ou JPG) para cada chácara diretamente pelo painel.
   - Atualização instantânea da foto no frontend sem necessidade de reiniciar a aplicação.

5. **Segurança e Arquitetura de Produção:**
   - **Autenticação Segura:** BCrypt para hash de senhas e sessões com cookies protegidos (`HttpOnly`, `SameSite=Lax`, `Secure` em HTTPS).
   - **Proteção Web:** Headers de segurança HTTP (`X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`).
   - **Suporte Multi-Banco:** H2 local para desenvolvimento zero-configuração e suporte automático a PostgreSQL no Render ou Neon via `DATABASE_URL`.
   - **Terminação SSL:** `server.forward-headers-strategy: framework` para proxy reverso do Render.

---

## 🛠️ Tecnologias Utilizadas

- **Backend:** Java 21, Spring Boot 3.3.4, Spring Security, Spring Data JPA / Hibernate
- **Banco de Dados:** H2 Database (Local) / PostgreSQL (Produção no Render)
- **Frontend:** HTML5, CSS3 Moderno, JavaScript ES6+, Tailwind CSS
- **Container & CI/CD:** Dockerfile multi-stage (Eclipse Temurin 21 Alpine), Maven Wrapper (`mvnw`), Render Blueprint (`render.yaml`)

---

## 🚀 Como Executar Localmente

### Pré-requisitos
- JDK 21 instalado e configurado no PATH (ou JAVA_HOME apontado).

### Inicialização rápida:
```powershell
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run

# Linux / macOS / Bash
./mvnw spring-boot:run
```

Acesse no navegador:
```
http://localhost:8080
```

### Credenciais Padrão:
| Perfil | E-mail | Senha |
|---|---|---|
| **Administrador** | `admin@chacaras.com` | `admin123` |
| **Usuário Comum** | `usuario@teste.com` | `user1234` |

---

## 🧪 Como Rodar os Testes Automatizados

O projeto conta com suíte de testes unitários e de integração cobrindo cenários de reserva de dia único, múltiplos dias consecutivos, detecção de conflitos e isolamento:

```powershell
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

---

## 🐙 Como Subir para o GitHub

1. Abra o terminal na pasta do projeto e inicialize o Git:
```powershell
git init
git add .
git commit -m "feat: agendamento de chacaras com tema clean, reservas consecutivas e deploy render"
```

2. Crie um novo repositório no seu GitHub (ex: `agendamento-chacaras`).

3. Vincule e envie o código:
```powershell
git branch -M main
git remote add origin https://github.com/SEU-USUARIO/agendamento-chacaras.git
git push -u origin main
```

---

## ☁️ Como Publicar no Render

O projeto está 100% adaptado para a infraestrutura do **Render** ([render.com](https://render.com)).

### Opção 1: Via Blueprint (`render.yaml`) — Modo Automático (Recomendado)
1. Acesse o seu painel no [Render Dashboard](https://dashboard.render.com).
2. Clique em **New +** > **Blueprint**.
3. Conecte o repositório que você acabou de subir no GitHub.
4. O Render detectará automaticamente o arquivo `render.yaml` na raiz e provisionará:
   - O **Web Service Docker** configurado com Java 21;
   - O **PostgreSQL Gerenciado Gratuito** (`agendamento-db`) já linkado via `DATABASE_URL`.
5. Clique em **Apply** e aguarde o build finalizar!

### Opção 2: Manualmente via Web Service Docker
1. No Render, clique em **New +** > **Web Service**.
2. Conecte seu repositório GitHub.
3. Defina as configurações:
   - **Runtime:** `Docker`
   - **Region:** `Oregon (US West)` ou de sua preferência
   - **Plan:** `Free`
4. Em **Environment Variables**, configure:
   - `PORT`: `10000`
   - `COOKIE_SECURE`: `true`
   - `DATABASE_URL`: *(sua connection string do PostgreSQL no Render ou Neon)*
   - `ADMIN_EMAIL`: `admin@chacaras.com` (opcional)
   - `ADMIN_PASSWORD`: *(sua senha administrativa segura)*
5. Clique em **Create Web Service**.
