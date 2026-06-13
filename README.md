# Esttufa Mobile

## Purpose

Aplicativo Android para autenticar usuários, cadastrar estufas e acompanhar
informações de culturas e irrigação consumindo a API Esttufa.

## Current Understanding

- Goal: integrar Firebase Auth e o CRUD autenticado de estufas.
- Users/actors: pessoas que monitoram suas próprias estufas.
- Constraints: Kotlin, Android SDK 34+, Views/View Binding e API REST existente.
- Integrations: Firebase Authentication e `https://api-esttufa.onrender.com/`.
- Non-goals: persistência direta no Firestore pelo app e redesign das telas.
- Assumptions: a API segue os contratos definidos em
  `specs/1_PLANNING_SPEC.md`.

## Architecture Overview

O projeto usa MVVM leve: Activities renderizam estado e encaminham ações,
ViewModels coordenam operações assíncronas, repositories encapsulam chamadas
Retrofit e a camada `model` contém contratos HTTP, DTOs e interceptores.

O token Firebase é anexado no OkHttp antes de cada chamada. Respostas `401`
invalidam a sessão e retornam o usuário ao login. Erros de validação da API são
traduzidos em mensagens legíveis na camada de aplicação.

Detailed module and file map: `PROJECT_SPEC.md`

## Project Pattern

- Chosen pattern: MVVM leve com separação presentation/data.
- Why it fits: o app tem poucas telas e regras, mas integra autenticação e API.
- Clean Code defaults: responsabilidades focadas, estados explícitos e erros
  tratados nas fronteiras.
- DDD/Clean Architecture boundaries: `Stove` é o conceito central; detalhes de
  Firebase e Retrofit não devem vazar para Activities.
- TDD strategy: testes unitários focados em mapeamentos e validações quando
  desacoplados do Android; build e smoke test para integrações externas.

## Epics and User Stories

### Epic 1: Sessão autenticada

#### Story 1.1: Configurar Firebase Auth

Status: Done
Started at: 2026-06-13 18:54

Acceptance criteria:

- Firebase Auth e Google Tasks disponíveis no módulo Android.
- O projeto compila com a configuração Firebase existente.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-13.

#### Story 1.2: Anexar token Firebase às requisições

Status: Done
Started at: 2026-06-13 19:05

Acceptance criteria:

- Usuários autenticados enviam `Authorization: Bearer <token>`.
- Requisições sem usuário continuam sem o header.
- Falhas ao obter token não interrompem rotas públicas.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-13.

#### Story 1.3: Integrar autenticação ao Retrofit

Status: Done
Started at: 2026-06-13 19:08

Acceptance criteria:

- O interceptor de autenticação executa antes do logging.
- Todas as chamadas do `ApiService` compartilham a política de token.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-13.

#### Story 1.4: Encerrar sessão em resposta 401

Status: Done
Started at: 2026-06-13 19:10

Acceptance criteria:

- Uma resposta `401` encerra a sessão Firebase.
- O app limpa a pilha autenticada e abre a tela de login.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-13.

### Epic 2: Contrato de estufas

#### Story 2.1: Mapear requests e responses de estufas

Status: Processing
Started at: 2026-06-13 19:10

Acceptance criteria:

- Requests de criação/edição e responses completos são desserializados.
- A lista `{ "stoves": [...] }` é mapeada corretamente.

Validation:

- Pending

#### Story 2.2: Expor endpoints CRUD no Retrofit

Status: Processing
Started at: 2026-06-13 19:10

Acceptance criteria:

- `POST`, `GET`, `GET/{id}`, `PUT/{id}` e `DELETE/{id}` estão disponíveis.

Validation:

- Pending

#### Story 2.3: Encapsular CRUD no repository

Status: Planned
Started at: Pending

Acceptance criteria:

- Todas as operações retornam `Result`.
- Exceções HTTP e de transporte são preservadas para tratamento da UI.

Validation:

- Pending

### Epic 3: Autenticação de usuário

#### Story 3.1: Entrar com email e senha

Status: Planned
Started at: Pending

Acceptance criteria:

- Campos obrigatórios são validados.
- Login válido abre a Home e sessão existente faz auto-login.
- Falhas de autenticação exibem mensagem e reabilitam a ação.

Validation:

- Pending

#### Story 3.2: Criar conta e perfil

Status: Planned
Started at: Pending

Acceptance criteria:

- Todos os campos são obrigatórios e senhas devem coincidir.
- A conta é criada e `displayName` recebe nome e sobrenome.
- Sucesso abre a Home; falha mantém o usuário no cadastro.

Validation:

- Pending

### Epic 4: Gestão de estufas

#### Story 4.1: Criar estufa pela API

Status: Planned
Started at: Pending

Acceptance criteria:

- Nome e cultura são obrigatórios.
- Cultura em português é convertida para o valor aceito pela API.
- Sucesso retorna `RESULT_OK`; erro `422` é apresentado de forma legível.

Validation:

- Pending

#### Story 4.2: Listar estufas do usuário

Status: Planned
Started at: Pending

Acceptance criteria:

- A Home usa `GET /stoves`.
- Nome da estufa e imagem local da cultura são renderizados.
- Lista vazia exibe o empty state existente.

Validation:

- Pending

#### Story 4.3: Atualizar Home e abrir detalhes

Status: Planned
Started at: Pending

Acceptance criteria:

- A lista recarrega ao voltar da criação.
- O clique envia `id` e `crop` para a tela de informações.
- A saudação usa o `displayName` quando disponível.

Validation:

- Pending

#### Story 4.4: Remover contrato legado

Status: Planned
Started at: Pending

Acceptance criteria:

- Modelos `Cultura` antigos e `GET /stoves/list` são removidos.
- Nomes de telas legados podem permanecer sem carregar o DTO antigo.

Validation:

- Pending

### Epic 5: Verificação

#### Story 5.1: Validar build e fluxo integrado

Status: Planned
Started at: Pending

Acceptance criteria:

- `assembleDebug` conclui sem erros.
- Fluxos de autenticação, listagem, criação e detalhes são verificados.
- Limitações de teste externo são registradas.

Validation:

- Pending

## Decision Log

- 2026-06-13: manter MVVM leve e os layouts existentes para limitar o escopo.
- 2026-06-13: usar o refresh automático de `getIdToken(false)` do Firebase.
- 2026-06-13: tratar `401` como evento transversal de encerramento de sessão.
