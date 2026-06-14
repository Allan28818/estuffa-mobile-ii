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

Status: Done
Started at: 2026-06-13 19:10

Acceptance criteria:

- Requests de criação/edição e responses completos são desserializados.
- A lista `{ "stoves": [...] }` é mapeada corretamente.

Validation:

- Contratos Kotlin revisados contra a spec em 2026-06-13.

#### Story 2.2: Expor endpoints CRUD no Retrofit

Status: Done
Started at: 2026-06-13 19:10

Acceptance criteria:

- `POST`, `GET`, `GET/{id}`, `PUT/{id}` e `DELETE/{id}` estão disponíveis.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-13.

#### Story 2.3: Encapsular CRUD no repository

Status: Done
Started at: 2026-06-13 19:19

Acceptance criteria:

- Todas as operações retornam `Result`.
- Exceções HTTP e de transporte são preservadas para tratamento da UI.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-13.

### Epic 3: Autenticação de usuário

#### Story 3.1: Entrar com email e senha

Status: Done
Started at: 2026-06-13 19:23

Acceptance criteria:

- Campos obrigatórios são validados.
- Login válido abre a Home e sessão existente faz auto-login.
- Falhas de autenticação exibem mensagem e reabilitam a ação.

Validation:

- `gradlew.bat assembleDebug --console=plain --stacktrace`: BUILD SUCCESSFUL.

#### Story 3.2: Criar conta e perfil

Status: Done
Started at: 2026-06-13 19:23

Acceptance criteria:

- Todos os campos são obrigatórios e senhas devem coincidir.
- A conta é criada e `displayName` recebe nome e sobrenome.
- Sucesso abre a Home; falha mantém o usuário no cadastro.

Validation:

- `gradlew.bat assembleDebug --console=plain --stacktrace`: BUILD SUCCESSFUL.

### Epic 4: Gestão de estufas

#### Story 4.1: Criar estufa pela API

Status: Done
Started at: 2026-06-13 19:22

Acceptance criteria:

- Nome e cultura são obrigatórios.
- Cultura em português é convertida para o valor aceito pela API.
- Sucesso retorna `RESULT_OK`; erro `422` é apresentado de forma legível.

Validation:

- `gradlew.bat assembleDebug --console=plain --stacktrace`: BUILD SUCCESSFUL
  com ViewModel e Activity integrados.

#### Story 4.2: Listar estufas do usuário

Status: Done
Started at: 2026-06-13 19:44

Acceptance criteria:

- A Home usa `GET /stoves`.
- Nome da estufa e imagem local da cultura são renderizados.
- Lista vazia exibe o empty state existente.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL.

#### Story 4.3: Atualizar Home e abrir detalhes

Status: Done
Started at: 2026-06-13 19:47

Acceptance criteria:

- A lista recarrega ao voltar da criação.
- O clique envia `id` e `crop` para a tela de informações.
- A saudação usa o `displayName` quando disponível.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL.

#### Story 4.4: Remover contrato legado

Status: Done
Started at: 2026-06-13 19:51

Acceptance criteria:

- Modelos `Cultura` antigos e `GET /stoves/list` são removidos.
- Nomes de telas legados podem permanecer sem carregar o DTO antigo.

Validation:

- Busca sem referências a `stoves/list`, DTO `Cultura` ou Glide.
- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL.

### Epic 5: Verificação

#### Story 5.1: Validar build e fluxo integrado

Status: Done
Started at: 2026-06-13 19:58

Acceptance criteria:

- `assembleDebug` conclui sem erros.
- Fluxos de autenticação, listagem, criação e detalhes são verificados.
- Limitações de teste externo são registradas.

Validation:

- `gradlew.bat testDebugUnitTest --console=plain`: BUILD SUCCESSFUL.
- `gradlew.bat lintDebug --console=plain`: BUILD SUCCESSFUL.
- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL.
- OpenAPI de produção confirmou rotas, Bearer auth, DTOs e culturas.
- Smoke REST confirmou `401`, `201`, list/get/update/delete, `422` e irrigação.
- Smoke no emulador confirmou validações, login, auto-login, empty state,
  criação, recarga, imagem local e detalhes com irrigação.
- Conta e estufas temporárias removidas após os testes.

### Epic 6: Classificação de imagem e warming da API

#### Story 6.1: Expor contratos de classificação

Status: Done
Started at: 2026-06-14 15:28

Acceptance criteria:

- A API expõe `POST /plant-classification/predict` como multipart.
- A imagem usa o campo `image` e o modelo padrão é `decision_tree`.
- A resposta aceita nomes de classe e confiança opcionais.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 6.2: Pré-aquecer a API no startup

Status: Done
Started at: 2026-06-14 15:28

Acceptance criteria:

- Login e Home solicitam o warming sem bloquear a interface.
- Apenas uma chamada é disparada por sessão do processo.
- Falhas de rede ou HTTP não geram feedback nem crash.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 6.3: Classificar imagens pela API real

Status: Done
Started at: 2026-06-14 15:28

Acceptance criteria:

- Fotos da câmera e imagens da galeria são enviadas à API.
- Loading impede um segundo envio e preserva feedback visual.
- Sucesso mostra imagem e classe; falha mostra o card de erro.
- A simulação baseada em `Random` deixa de existir.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.
- Busca estrutural confirmou ausência de `Random` no fluxo de classificação.

#### Story 6.4: Validar integração de imagem e warming

Status: Done
Started at: 2026-06-14 15:28

Acceptance criteria:

- `assembleDebug` conclui sem erros.
- Os fluxos de irrigação existentes continuam compilando e sem regressão
  estrutural.
- As limitações de smoke test externo ficam documentadas.

Validation:

- `gradlew.bat testDebugUnitTest --console=plain`: BUILD SUCCESSFUL.
- `gradlew.bat lintDebug --console=plain`: BUILD SUCCESSFUL.
- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL.
- O sandbox não alcançou `https://api-esttufa.onrender.com/hearth-beat`.
- O Android SDK local não possui `platform-tools/adb`; smoke em dispositivo
  não foi executado neste ambiente.

### Epic 7: Perfil e planos do serviço

#### Story 7.1: Modelar perfil, plano e configuração

Status: Done
Started at: 2026-06-14 15:53

Acceptance criteria:

- Perfil representa dados Firebase e assinatura local.
- Plano representa preço, benefícios, recomendação e apresentação.
- Item de configuração representa ícone, rótulo e ação destrutiva.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.2: Fornecer perfil e catálogo local

Status: Done
Started at: 2026-06-14 15:58

Acceptance criteria:

- Perfil é montado a partir do Firebase Auth e mocks definidos na spec.
- O catálogo contém os cinco planos e o plano atual local.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.3: Publicar estado do perfil

Status: Done
Started at: 2026-06-14 16:03

Acceptance criteria:

- Loading, sucesso e erro são explícitos.
- Logout e itens de configuração são expostos pelo ViewModel.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.4: Publicar estado e eventos dos planos

Status: Done
Started at: 2026-06-14 16:05

Acceptance criteria:

- Lista, plano atual, expansão e eventos de assinatura são observáveis.
- A confirmação atualiza o plano apenas localmente.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.5: Criar recursos visuais compartilhados

Status: Done
Started at: 2026-06-14 16:00

Acceptance criteria:

- Ícones, fundos, cores e textos exigidos pelas telas estão disponíveis.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.6: Estruturar a tela de perfil

Status: Done
Started at: 2026-06-14 16:07

Acceptance criteria:

- Layout contempla cabeçalho, informações, assinatura, configurações e estados.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.7: Estruturar planos e confirmação

Status: Done
Started at: 2026-06-14 16:11

Acceptance criteria:

- Layout contempla lista, cards, estados e bottom sheet.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.8: Renderizar configurações

Status: Done
Started at: 2026-06-14 16:16

Acceptance criteria:

- Adapter vincula ícones, rótulos, chevrons e ação destrutiva.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.9: Renderizar cards de planos

Status: Done
Started at: 2026-06-14 16:18

Acceptance criteria:

- Adapter renderiza plano atual, recomendação, expansão e assinatura.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.10: Implementar o fluxo de perfil

Status: Done
Started at: 2026-06-14 16:22

Acceptance criteria:

- Perfil renderiza dados e estados, abre planos e confirma logout.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.11: Implementar o fluxo de planos

Status: Done
Started at: 2026-06-14 16:26

Acceptance criteria:

- Planos renderiza o catálogo e confirma assinatura pelo bottom sheet.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.12: Conectar a Home ao perfil

Status: Done
Started at: 2026-06-14 16:30

Acceptance criteria:

- Avatar/cabeçalho abre o perfil e as Activities estão registradas.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-14.

#### Story 7.13: Validar perfil e planos

Status: Planned

Acceptance criteria:

- Build e lint passam sem erros críticos.
- Fluxo Home -> Perfil -> Planos e retorno está coerente.

## Decision Log

- 2026-06-13: manter MVVM leve e os layouts existentes para limitar o escopo.
- 2026-06-13: forçar `getIdToken(true)` para cumprir o contrato de token
  atualizado definido pela integração.
- 2026-06-13: tratar `401` como evento transversal de encerramento de sessão.
- 2026-06-14: manter a classificação no fluxo MVVM existente e isolar o
  warming em um helper de processo idempotente.
- 2026-06-14: manter perfil e assinatura como domínio local nesta iteração,
  usando Firebase Auth apenas como fonte de identidade.
