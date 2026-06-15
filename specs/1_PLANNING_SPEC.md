# Planning Spec: Integração Firebase Auth + CRUD de Estufas via API REST (App Kotlin)
**Status**: DONE
**Started at**: 2026-06-13 18:49:39
**Priority**: high

> [!IMPORTANT]
> **Instructions for the Executing AI**:
> You MUST update the `Status` field above as you progress through this implementation:
> - **TO DO**: The specification is created but execution has not started.
> - **PROCESSING**: Update to this status as soon as you begin work on the first task. You must also record the date and time when this status is set. Note: Specs marked as PROCESSING can be picked up again if they have been in this status for more than 1 hour (the executing AI already knows how to verify this).
> - **DONE**: Update to this status when all tasks are fully completed and verified.
> - **BUG**: take this spec again. It's not working and need to be tested. If possible, ask to the user to discover which problems this spec has. Ensure that the reason for the bug is clearly documented so the AI understands exactly what to fix.
> Do not begin implementation without updating the status to `PROCESSING`.

> [!CAUTION]
> **A pasta `./specs` NÃO deve ser commitada no repositório.** Ela é um diretório de planejamento local, usado exclusivamente para coordenação entre agentes e desenvolvedores. **NÃO adicione `./specs` ao `.gitignore`** — ela deve permanecer como um diretório untracked por convenção.

---

## 1. Project Context & Stack
- **Languages & Runtime**: Kotlin (sem plugin `kotlin-android` explícito — usa AGP 9.0.1 que embute suporte Kotlin), Java 11 target
- **Core Frameworks**: Android SDK (compileSdk 36, minSdk 34, targetSdk 36), Material Design 3, AndroidX
- **Networking**: Retrofit 2.9.0 + Gson Converter + OkHttp 4.11.0 (LoggingInterceptor)
- **Image Loading**: Glide 4.16.0
- **Async**: Kotlin Coroutines 1.7.3 + ViewModel KTX (lifecycle-viewmodel-ktx 2.10.0)
- **Firebase**: BOM 34.14.1 já importado no `build.gradle.kts`, `google-services.json` presente com projeto `esttufa-ai`. **Nenhum módulo Firebase individual declarado** (firebase-auth, etc.) — apenas o BOM.
- **Build**: View Binding habilitado. Sem Compose, sem Navigation Component, sem Hilt/Dagger.
- **Testing Infrastructure**: JUnit 4 + AndroidX Test + Espresso. Nenhum teste customizado existente.
- **API de produção**: `https://api-esttufa.onrender.com/` (Render) — base URL já configurada em `RetrofitClient.kt`
- **Observed Conventions & Patterns**:
  - Arquitetura MVVM leve: `Activity` → `ViewModel` → `Repository` → `RetrofitClient` (singleton) → `ApiService` (interface Retrofit)
  - Pacote base: `com.example.esttufa`
  - Subpacotes: `model/`, `viewmodel/`, `repository/`, `adapter/`
  - ViewModels usam `LiveData` + `viewModelScope.launch` com coroutines
  - Repositories retornam `Result<T>` (Kotlin Result wrapper)
  - Activities usam View Binding (`ActivityXxxBinding.inflate()`)
  - Login/Cadastro (`MainActivity`, `CadastroActivity`) são **placeholders sem integração real** — apenas navegam para `HomeActivity` ao clicar nos botões
  - `HomeActivity` carrega lista de estufas via `GET /stoves/list` (rota mock da API) e exibe em `ListView` com `CulturaAdapter`
  - `CadastroEstufaActivity` tem formulário com nome + dropdown de cultura, mas o botão "Criar" apenas exibe Toast e fecha a Activity (sem API call)
- **Estado atual das telas**:
  - `MainActivity` (Login): campos email/senha, botões Login e Cadastrar — **sem Firebase Auth**
  - `CadastroActivity` (Registro): campos nome, sobrenome, email, senha, confirmar senha — **sem Firebase Auth**
  - `HomeActivity`: lista estufas via API mock, FAB para cadastrar estufa
  - `CadastroEstufaActivity`: formulário nome + cultura dropdown, botão criar — **sem chamada à API**
  - `CulturaInfoActivity`: exibe dados de irrigação com sensores/câmera
- **Modelo de dados atual (`Cultura.kt`)**: `id: String`, `name: String`, `url: String` — mapeado do mock `GET /stoves/list`
- **Risks & Fragile Areas**:
  - Login/Cadastro são **totalmente placeholders** — a transição para Firebase Auth requer cuidado com fluxo de navegação e tratamento de erros
  - O modelo `Cultura` atual tem campos `id/name/url` que serão substituídos por `id/name/crop/user_id/created_at/updated_at` do CRUD real
  - A rota `GET /stoves/list` será substituída por `GET /stoves` (protegida por auth) conforme `2_PLANNING_SPEC.md` da API
  - O `RetrofitClient` atual NÃO envia header `Authorization` — precisa de interceptor para token Firebase
  - Todas as chamadas à API que já existem (irrigação, culturas) passarão a exigir token após a API implementar auth global
  - O `CulturaAdapter` depende de `Cultura.url` para carregar imagem com Glide — o novo modelo `StoveResponse` não tem `url`, precisará adaptar o UI

---

## 2. Requirements & Acceptance Criteria
- **Goal**: Integrar autenticação Firebase (login/cadastro com email e senha) no app Kotlin e implementar o CRUD de estufas consumindo as novas rotas da API de produção no Render (`POST /stoves`, `GET /stoves`, `GET /stoves/{id}`, `PUT /stoves/{id}`, `DELETE /stoves/{id}`). Todas as requisições à API devem enviar o Firebase ID Token no header `Authorization: Bearer <token>`.

- **Acceptance Criteria**:

  ### Autenticação Firebase (Client-Side)
  - `MAUTH-01`: WHEN o usuário preenche email e senha válidos na tela de Login e clica em "Entrar" THEN o app SHALL autenticar via `FirebaseAuth.signInWithEmailAndPassword()`, navegar para `HomeActivity` em caso de sucesso, e exibir mensagem de erro em caso de falha (credenciais inválidas, usuário não encontrado, etc.).
  - `MAUTH-02`: WHEN o usuário preenche nome, sobrenome, email, senha e confirmação de senha na tela de Cadastro e clica em "Cadastrar" THEN o app SHALL criar conta via `FirebaseAuth.createUserWithEmailAndPassword()`, atualizar o displayName do usuário com nome+sobrenome via `updateProfile()`, navegar para `HomeActivity` em caso de sucesso, e exibir mensagem de erro em caso de falha.
  - `MAUTH-03`: WHEN o usuário está logado e o app faz qualquer requisição HTTP à API THEN o app SHALL obter o ID Token atualizado via `user.getIdToken(true)` e adicioná-lo como header `Authorization: Bearer <token>` em TODAS as requisições Retrofit.
  - `MAUTH-04`: WHEN a senha e confirmação de senha não coincidem na tela de Cadastro THEN o app SHALL exibir mensagem de erro e NÃO chamar a API do Firebase.
  - `MAUTH-05`: WHEN qualquer campo obrigatório (email, senha) está vazio na tela de Login ou Cadastro THEN o app SHALL exibir mensagem de erro nos campos correspondentes e NÃO prosseguir.

  ### Interceptor de Token no Retrofit
  - `MTOKEN-01`: WHEN o `RetrofitClient` é inicializado THEN o app SHALL configurar um `Interceptor` do OkHttp que automaticamente adiciona o header `Authorization: Bearer <firebase_id_token>` a TODAS as requisições.
  - `MTOKEN-02`: WHEN o token Firebase está expirado ou indisponível THEN o interceptor SHALL buscar um token atualizado via `getIdToken(true)` de forma síncrona (usando `Tasks.await()`) antes de adicionar ao header.
  - `MTOKEN-03`: WHEN o usuário NÃO está logado (no `currentUser`) THEN as requisições SHALL ser enviadas SEM o header `Authorization` (para não bloquear rotas públicas como healthcheck, se houver).

  ### CRUD de Estufas via API REST
  - `MSTOVE-01`: WHEN o usuário autenticado está na `CadastroEstufaActivity`, preenche nome e seleciona cultura, e clica em "Criar Estufa" THEN o app SHALL enviar `POST /stoves` com body `{ "name": "<nome>", "crop": "<cultura_em_ingles>" }` e, em caso de sucesso (201), exibir Toast de confirmação e retornar à `HomeActivity`.
  - `MSTOVE-02`: WHEN o `HomeActivity.onCreate()` é chamado THEN o app SHALL chamar `GET /stoves` (que retorna apenas estufas do usuário autenticado) e exibir a lista no `ListView`.
  - `MSTOVE-03`: WHEN o usuário clica em uma estufa na lista da Home THEN o app SHALL navegar para `CulturaInfoActivity` passando o `id` da estufa e o `crop` como extras.
  - `MSTOVE-04`: WHEN a API retorna erro `401 Unauthorized` em qualquer requisição THEN o app SHALL fazer logout do Firebase Auth e redirecionar o usuário para a tela de Login (`MainActivity`).
  - `MSTOVE-05`: WHEN a API retorna erro `422 Unprocessable Entity` no `POST /stoves` THEN o app SHALL exibir mensagem de erro legível ao usuário.
  - `MSTOVE-06`: WHEN a lista de estufas retornada pela API está vazia THEN o app SHALL exibir o empty state existente (`llEmptyState`).

  ### Adaptação do Modelo de Dados
  - `MMODEL-01`: WHEN a API retorna a resposta de `GET /stoves` THEN o modelo `Cultura` (ou novo modelo `Stove`) SHALL mapear os campos `id`, `name`, `crop`, `user_id`, `created_at` e `updated_at` corretamente.
  - `MMODEL-02`: WHEN o `CulturaAdapter` renderiza um item da lista THEN ele SHALL exibir o nome da estufa e usar uma imagem local (drawable) baseada no campo `crop` ao invés de carregar URL remota.

---

## 3. Design & Impact Analysis

### Fluxo de Autenticação
```
┌─────────────────────┐
│   MainActivity      │
│   (Tela de Login)   │
│                     │
│ email + senha       │
│ [Entrar] [Cadastrar]│
└──────┬──────────────┘
       │                     ┌──────────────────────┐
       │── clica Cadastrar──►│  CadastroActivity     │
       │                     │  (Tela de Registro)   │
       │                     │                       │
       │                     │ nome, sobrenome,      │
       │                     │ email, senha, confirm │
       │                     │ [Cadastrar]           │
       │                     └──────┬────────────────┘
       │                            │
       ▼                            ▼
  FirebaseAuth                 FirebaseAuth
  .signInWith                  .createUserWith
  EmailAndPassword()           EmailAndPassword()
       │                            │
       └──────────┬─────────────────┘
                  ▼
          ┌───────────────┐
          │  HomeActivity  │
          │  (autenticado) │
          └───────────────┘
```

### Fluxo de Requisição com Token
```
ViewModel.loadStoves()
    │
    ▼
Repository.getStoves()
    │
    ▼
RetrofitClient.api.getStoves()
    │
    ▼
OkHttp Interceptor (AuthInterceptor)
    ├─ FirebaseAuth.currentUser?.getIdToken(false)
    ├─ Tasks.await(tokenTask)
    ├─ request.newBuilder().addHeader("Authorization", "Bearer $token")
    │
    ▼
API Render: GET https://api-esttufa.onrender.com/stoves
    ├─ Header: Authorization: Bearer <id_token>
    │
    ▼
Response: { "stoves": [...] }
```

### Mapeamento cultura PT → EN (para o POST)
```
UI Dropdown          →  API crop value
"Alface"             →  "lettuce"
"Tomate"             →  "tomato"
"Rúcula"             →  "arugula"
```

### Code Reuse Blueprints
- **Padrão de Repository**: `repository/IrrigationRepository.kt` — blueprint de repository com `Result<T>` wrapper
- **Padrão de ViewModel**: `viewmodel/HomeViewModel.kt` — blueprint de ViewModel com LiveData + viewModelScope
- **Padrão de UiState**: `viewmodel/CulturaInfoViewModel.kt` — blueprint de sealed class para estados (Loading/Success/Error)
- **Padrão de Adapter**: `adapter/CulturaAdapter.kt` — blueprint de ArrayAdapter com layout customizado
- **Padrão de Activity**: `CadastroEstufaActivity.kt` — blueprint de Activity com View Binding e setupUI

### Files to Modify
- `app/build.gradle.kts`: Adicionar dependências `firebase-auth` e `firebase-firestore` (opcionalmente apenas auth, pois Firestore é na API)
- `app/src/main/java/com/example/esttufa/model/RetrofitClient.kt`: Adicionar `AuthInterceptor` ao OkHttpClient
- `app/src/main/java/com/example/esttufa/model/ApiService.kt`: Adicionar endpoints CRUD de estufas (POST, GET, GET/{id}, PUT/{id}, DELETE/{id})
- `app/src/main/java/com/example/esttufa/model/Cultura.kt`: Atualizar campos para refletir `StoveResponse` da API ou criar novo model
- `app/src/main/java/com/example/esttufa/model/CulturaResponse.kt`: Atualizar para mapear novo response da API
- `app/src/main/java/com/example/esttufa/repository/CulturaRepository.kt`: Atualizar para usar nova rota `GET /stoves` ao invés de `GET /stoves/list`
- `app/src/main/java/com/example/esttufa/viewmodel/HomeViewModel.kt`: Ajustar para novo modelo de dados
- `app/src/main/java/com/example/esttufa/adapter/CulturaAdapter.kt`: Usar imagem local (drawable) baseada em `crop` ao invés de URL remota
- `app/src/main/java/com/example/esttufa/MainActivity.kt`: Implementar login real com Firebase Auth
- `app/src/main/java/com/example/esttufa/CadastroActivity.kt`: Implementar registro real com Firebase Auth
- `app/src/main/java/com/example/esttufa/CadastroEstufaActivity.kt`: Implementar chamada `POST /stoves` real via ViewModel
- `app/src/main/java/com/example/esttufa/HomeActivity.kt`: Ajustar para recarregar lista ao voltar de `CadastroEstufaActivity`

### New Files to Create
- `app/src/main/java/com/example/esttufa/model/AuthInterceptor.kt`: Interceptor OkHttp para injetar token Firebase
- `app/src/main/java/com/example/esttufa/model/StoveRequest.kt`: Data class para o body do `POST /stoves` e `PUT /stoves/{id}`
- `app/src/main/java/com/example/esttufa/model/StoveResponse.kt`: Data class para resposta completa de uma estufa (id, name, crop, user_id, created_at, updated_at)
- `app/src/main/java/com/example/esttufa/model/StoveListResponse.kt`: Data class wrapper `{ "stoves": [...] }`
- `app/src/main/java/com/example/esttufa/repository/StoveRepository.kt`: Repository para operações CRUD de estufas
- `app/src/main/java/com/example/esttufa/viewmodel/CadastroEstufaViewModel.kt`: ViewModel para a tela de criação de estufa
- `app/src/main/java/com/example/esttufa/viewmodel/LoginViewModel.kt`: ViewModel para autenticação (login)
- `app/src/main/java/com/example/esttufa/viewmodel/CadastroViewModel.kt`: ViewModel para registro de usuário

---

## 4. Execution Plan & Parallelism Map
```
Phase 1 (Sequential — Dependências e Interceptor):
  T1 ──→ T2 ──→ T3

Phase 2 (Parallel — Modelos e API Service):
  T3 complete, then:
    ├── T4 [P]   (StoveRequest, StoveResponse, StoveListResponse)
    └── T5 [P]   (ApiService — novos endpoints)

Phase 3 (Sequential — Repository e ViewModel de Estufas):
  T4, T5 complete, then:
    T6 ──→ T7

Phase 4 (Parallel — Autenticação + Adaptações UI):
  T7 complete, then:
    ├── T8 [P]   (LoginViewModel + MainActivity com Firebase Auth)
    ├── T9 [P]   (CadastroViewModel + CadastroActivity com Firebase Auth)
    └── T10 [P]  (CadastroEstufaViewModel + CadastroEstufaActivity com POST)

Phase 5 (Sequential — Wiring e Adapter):
  T8, T9, T10 complete, then:
    T11 ──→ T12 ──→ T13

Phase 6 (Sequential — Verificação Final):
  T13 complete, then:
    T14
```

> [!IMPORTANT]
> **Subagent Concurrency Rules**: For each task marked with `[P]`, the executing agent **MUST spawn a separate concurrent subagent** to perform the work. Sequential tasks (without `[P]`) should be executed inline or by single sequential subagents.

---

## 5. Granular Task Checklist

### T1: Adicionar dependências Firebase Auth ao `build.gradle.kts`
- **What**: Adicionar `firebase-auth` ao `build.gradle.kts` do módulo `app`. O BOM (`firebase-bom:34.14.1`) já está declarado, então basta adicionar o módulo individual sem versão.
- **Where**: `app/build.gradle.kts`
- **Depends on**: None
- **Reuses**: None
- **Requirement ID**: `MAUTH-01`, `MAUTH-02`, `MTOKEN-01`
- **Done when**:
  - [ ] `implementation("com.google.firebase:firebase-auth")` adicionado ao bloco `dependencies`
  - [ ] `implementation("com.google.android.gms:play-services-auth:21.3.0")` adicionado (necessário para `Tasks.await()`)
  - [ ] Gate check: Projeto compila sem erros (`./gradlew assembleDebug` ou sync no Android Studio)
- **Tests**: none
- **Gate**: quick
- **Commit**: `build(deps): add firebase-auth and play-services-auth dependencies`

---

### T2: Criar `AuthInterceptor` para injeção automática de token Firebase
- **What**: Criar um `Interceptor` do OkHttp que obtém o Firebase ID Token do usuário logado e adiciona como header `Authorization: Bearer <token>` a todas as requisições. Se não houver usuário logado, a requisição é enviada sem o header.
- **Where**: `app/src/main/java/com/example/esttufa/model/AuthInterceptor.kt`
- **Depends on**: T1
- **Reuses**: None
- **Requirement ID**: `MTOKEN-01`, `MTOKEN-02`, `MTOKEN-03`
- **Done when**:
  - [ ] Arquivo `AuthInterceptor.kt` criado no pacote `com.example.esttufa.model`
  - [ ] Classe `AuthInterceptor : Interceptor` implementada com método `intercept(chain: Interceptor.Chain): Response`
  - [ ] Dentro de `intercept()`:
    1. Obtém `FirebaseAuth.getInstance().currentUser`
    2. Se `currentUser != null`, chama `user.getIdToken(false)` e usa `Tasks.await(tokenTask)` para obter o token de forma síncrona (interceptors do OkHttp rodam em thread de IO)
    3. Adiciona `request.newBuilder().addHeader("Authorization", "Bearer $token").build()`
    4. Se `currentUser == null`, prossegue com a request original sem modificação
  - [ ] Try-catch para exceções de `Tasks.await()` — em caso de falha, prossegue sem token
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(network): add AuthInterceptor for Firebase ID token injection`

---

### T3: Integrar `AuthInterceptor` no `RetrofitClient`
- **What**: Adicionar o `AuthInterceptor` ao `OkHttpClient` existente no `RetrofitClient` para que todas as chamadas Retrofit enviem automaticamente o token Firebase.
- **Where**: `app/src/main/java/com/example/esttufa/model/RetrofitClient.kt`
- **Depends on**: T2
- **Reuses**: `RetrofitClient.kt` existente
- **Requirement ID**: `MTOKEN-01`, `MAUTH-03`
- **Done when**:
  - [ ] Import de `AuthInterceptor` adicionado
  - [ ] `AuthInterceptor()` adicionado ao `OkHttpClient.Builder()` via `.addInterceptor(AuthInterceptor())` ANTES do logging interceptor
  - [ ] O logging interceptor permanece como último interceptor para logar headers com o token
  - [ ] Gate check: Projeto compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(network): integrate AuthInterceptor into RetrofitClient`

---

### T4: Criar modelos de dados para Stove (Request/Response) [P]
- **What**: Criar data classes Kotlin para mapear o request body e response body das rotas CRUD de estufas da API.
- **Where**: `app/src/main/java/com/example/esttufa/model/StoveRequest.kt`, `app/src/main/java/com/example/esttufa/model/StoveResponse.kt`, `app/src/main/java/com/example/esttufa/model/StoveListResponse.kt`
- **Depends on**: T3
- **Reuses**: `Cultura.kt` e `CulturaResponse.kt` como referência de padrão
- **Requirement ID**: `MMODEL-01`, `MSTOVE-01`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] `StoveRequest.kt` criado com:
    - `data class CreateStoveRequest(val name: String, val crop: String)`
    - `data class UpdateStoveRequest(val name: String? = null, val crop: String? = null)`
  - [ ] `StoveResponse.kt` criado com:
    - `data class StoveResponse(val id: String, val name: String, val crop: String, val user_id: String, val created_at: String, val updated_at: String)`
  - [ ] `StoveListResponse.kt` criado com:
    - `data class StoveListResponse(val stoves: List<StoveResponse>)`
  - [ ] Modelo antigo `Cultura.kt` mantido temporariamente para compatibilidade (será removido no T11)
  - [ ] `CulturaResponse.kt` mantido temporariamente (será removido no T11)
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(models): add Stove request/response data classes for CRUD API`

---

### T5: Adicionar endpoints CRUD de estufas ao `ApiService` [P]
- **What**: Adicionar as definições Retrofit para os endpoints CRUD de estufas da API de produção.
- **Where**: `app/src/main/java/com/example/esttufa/model/ApiService.kt`
- **Depends on**: T3
- **Reuses**: Endpoints existentes em `ApiService.kt` como referência
- **Requirement ID**: `MSTOVE-01`, `MSTOVE-02`, `MSTOVE-03`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] Import de `retrofit2.http.Body`, `retrofit2.http.POST`, `retrofit2.http.PUT`, `retrofit2.http.DELETE` adicionados
  - [ ] Imports dos novos modelos adicionados
  - [ ] Endpoints adicionados:
    ```kotlin
    @POST("stoves")
    suspend fun createStove(@Body body: CreateStoveRequest): StoveResponse

    @GET("stoves")
    suspend fun getStoves(): StoveListResponse

    @GET("stoves/{stove_id}")
    suspend fun getStove(@Path("stove_id") stoveId: String): StoveResponse

    @PUT("stoves/{stove_id}")
    suspend fun updateStove(@Path("stove_id") stoveId: String, @Body body: UpdateStoveRequest): StoveResponse

    @DELETE("stoves/{stove_id}")
    suspend fun deleteStove(@Path("stove_id") stoveId: String)
    ```
  - [ ] Endpoint antigo `getCulturas()` mantido temporariamente (será removido no T11)
  - [ ] Gate check: Projeto compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(api): add CRUD stove endpoints to ApiService`

---

### T6: Criar `StoveRepository` para operações CRUD
- **What**: Criar repository que encapsula as chamadas Retrofit para o CRUD de estufas, seguindo o padrão `Result<T>` existente.
- **Where**: `app/src/main/java/com/example/esttufa/repository/StoveRepository.kt`
- **Depends on**: T4, T5
- **Reuses**: `IrrigationRepository.kt` (padrão de repository com Result<T>)
- **Requirement ID**: `MSTOVE-01`, `MSTOVE-02`, `MSTOVE-03`
- **Done when**:
  - [ ] Arquivo `StoveRepository.kt` criado no pacote `com.example.esttufa.repository`
  - [ ] Classe `StoveRepository` com métodos:
    - `suspend fun createStove(name: String, crop: String): Result<StoveResponse>` — chama `RetrofitClient.api.createStove(CreateStoveRequest(name, crop))`
    - `suspend fun getStoves(): Result<List<StoveResponse>>` — chama `RetrofitClient.api.getStoves()` e retorna `result.stoves`
    - `suspend fun getStove(stoveId: String): Result<StoveResponse>` — chama `RetrofitClient.api.getStove(stoveId)`
    - `suspend fun updateStove(stoveId: String, name: String?, crop: String?): Result<StoveResponse>` — chama `RetrofitClient.api.updateStove(stoveId, UpdateStoveRequest(name, crop))`
    - `suspend fun deleteStove(stoveId: String): Result<Unit>` — chama `RetrofitClient.api.deleteStove(stoveId)`
  - [ ] Todos os métodos envolvidos em `try-catch` com `Result.failure(e)`
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(repository): add StoveRepository with CRUD operations`

---

### T7: Criar `CadastroEstufaViewModel` para criação de estufa
- **What**: Criar ViewModel para gerenciar o estado da tela de criação de estufa, incluindo chamada à API e feedback de loading/success/error.
- **Where**: `app/src/main/java/com/example/esttufa/viewmodel/CadastroEstufaViewModel.kt`
- **Depends on**: T6
- **Reuses**: `CulturaInfoViewModel.kt` (padrão sealed class UiState + viewModelScope)
- **Requirement ID**: `MSTOVE-01`, `MSTOVE-05`
- **Done when**:
  - [ ] Arquivo `CadastroEstufaViewModel.kt` criado
  - [ ] Sealed class `CadastroEstufaUiState` com:
    - `object Idle : CadastroEstufaUiState()`
    - `object Loading : CadastroEstufaUiState()`
    - `data class Success(val stove: StoveResponse) : CadastroEstufaUiState()`
    - `data class Error(val message: String) : CadastroEstufaUiState()`
  - [ ] Classe `CadastroEstufaViewModel` com:
    - `private val repository = StoveRepository()`
    - `private val _uiState = MutableLiveData<CadastroEstufaUiState>(CadastroEstufaUiState.Idle)`
    - `val uiState: LiveData<CadastroEstufaUiState>`
    - Função `createStove(name: String, crop: String)` que:
      1. Seta estado para `Loading`
      2. Chama `repository.createStove(name, crop)` em `viewModelScope.launch`
      3. Em `onSuccess` seta estado para `Success`
      4. Em `onFailure` seta estado para `Error(message)`
  - [ ] Mapeamento da cultura PT→EN dentro da função ou como helper:
    - `"Alface"` → `"lettuce"`, `"Tomate"` → `"tomato"`, `"Rúcula"` → `"arugula"`
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(viewmodel): add CadastroEstufaViewModel with API creation logic`

---

### T8: Implementar login real com Firebase Auth na `MainActivity` [P]
- **What**: Substituir a navegação placeholder da `MainActivity` por autenticação real com Firebase Auth usando email e senha.
- **Where**: `app/src/main/java/com/example/esttufa/MainActivity.kt`
- **Depends on**: T7
- **Reuses**: Layout existente `activity_main.xml` (campos email e senha já presentes)
- **Requirement ID**: `MAUTH-01`, `MAUTH-05`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] Import de `com.google.firebase.auth.FirebaseAuth` adicionado
  - [ ] Instância `private lateinit var auth: FirebaseAuth` declarada e inicializada em `onCreate()` com `FirebaseAuth.getInstance()`
  - [ ] Verificação no `onCreate()`: se `auth.currentUser != null`, navega direto para `HomeActivity` e faz `finish()` (auto-login)
  - [ ] Botão `btnLogin` agora:
    1. Extrai email e senha dos campos do layout (View Binding — os IDs são `etEmail` e o `TextInputEditText` dentro de `tilSenha`)
    2. Valida que ambos não estão vazios — se vazios, exibe erro no campo (ex: `binding.tilEmail.error = "..."`)
    3. Chama `auth.signInWithEmailAndPassword(email, senha)`
    4. Em `addOnSuccessListener`: navega para `HomeActivity`, faz `finish()`
    5. Em `addOnFailureListener`: exibe `Toast` com mensagem de erro traduzida (credenciais inválidas, etc.)
  - [ ] Botão `btnCadastrar` (que já existe no layout como `btn_cadastrar`) continua navegando para `CadastroActivity`
  - [ ] ProgressBar ou desabilitar botão durante a chamada de autenticação
  - [ ] Gate check: Compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(auth): implement Firebase email/password login in MainActivity`

---

### T9: Implementar registro real com Firebase Auth na `CadastroActivity` [P]
- **What**: Substituir a navegação placeholder da `CadastroActivity` por registro real com Firebase Auth, incluindo validação de campos e atualização de perfil (displayName).
- **Where**: `app/src/main/java/com/example/esttufa/CadastroActivity.kt`
- **Depends on**: T7
- **Reuses**: Layout existente `activity_cadastro.xml` (campos nome, sobrenome, email, senha, confirmar senha já presentes)
- **Requirement ID**: `MAUTH-02`, `MAUTH-04`, `MAUTH-05`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] View Binding implementado (atualmente usa `findViewById`) — migrar para `ActivityCadastroBinding.inflate()`
  - [ ] Import de `com.google.firebase.auth.FirebaseAuth` e `com.google.firebase.auth.userProfileChangeRequest` adicionados
  - [ ] Instância `private lateinit var auth: FirebaseAuth` inicializada
  - [ ] Botão `btnCadastrar` agora:
    1. Extrai nome, sobrenome, email, senha e confirmação dos campos
    2. Valida todos os campos não vazios
    3. Valida que senha == confirmação de senha — se diferente, exibe erro `"As senhas não coincidem"`
    4. Chama `auth.createUserWithEmailAndPassword(email, senha)`
    5. Em `addOnSuccessListener`:
       - Obtém `auth.currentUser`
       - Chama `user.updateProfile(userProfileChangeRequest { displayName = "$nome $sobrenome" })`
       - Navega para `HomeActivity`, faz `finish()`
    6. Em `addOnFailureListener`: exibe Toast com erro
  - [ ] Botão `btnLogin` continua fazendo `finish()` (volta para MainActivity)
  - [ ] ProgressBar ou desabilitar botão durante a chamada
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(auth): implement Firebase email/password registration in CadastroActivity`

---

### T10: Integrar `CadastroEstufaActivity` com ViewModel e API POST [P]
- **What**: Conectar a `CadastroEstufaActivity` ao `CadastroEstufaViewModel` para que o botão "Criar Estufa" envie `POST /stoves` à API com o nome e a cultura selecionada.
- **Where**: `app/src/main/java/com/example/esttufa/CadastroEstufaActivity.kt`
- **Depends on**: T7
- **Reuses**: `CadastroEstufaActivity.kt` existente (layout e dropdown já funcionais)
- **Requirement ID**: `MSTOVE-01`, `MSTOVE-05`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] Import de `CadastroEstufaViewModel` e `CadastroEstufaUiState` adicionados
  - [ ] `private val viewModel: CadastroEstufaViewModel by viewModels()` declarado
  - [ ] Método `observeViewModel()` implementado observando `viewModel.uiState`:
    - `Idle`: nenhuma ação
    - `Loading`: exibir ProgressBar ou desabilitar botão
    - `Success`: Toast "Estufa criada com sucesso!", `setResult(RESULT_OK)`, `finish()`
    - `Error`: Toast com mensagem de erro, reabilitar botão
  - [ ] Botão `btnCriarEstufa` agora chama `viewModel.createStove(nome, culturaPT)` ao invés de exibir Toast diretamente
  - [ ] Mapeamento PT→EN feito no ViewModel (T7), Activity envia o valor do dropdown como está
  - [ ] Gate check: Compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(cadastro-estufa): integrate with CadastroEstufaViewModel for API POST`

---

### T11: Atualizar `CulturaAdapter`, `HomeViewModel` e `CulturaRepository` para novo modelo
- **What**: Adaptar a cadeia Repository → ViewModel → Adapter da Home para usar o novo modelo `StoveResponse` e a nova rota `GET /stoves` (sem `/list`). O adapter deve usar imagens locais (drawables) baseadas no campo `crop` ao invés de carregar URLs remotas.
- **Where**: `adapter/CulturaAdapter.kt`, `viewmodel/HomeViewModel.kt`, `repository/CulturaRepository.kt`
- **Depends on**: T8, T9, T10
- **Reuses**: Código existente dos 3 arquivos
- **Requirement ID**: `MMODEL-01`, `MMODEL-02`, `MSTOVE-02`, `MSTOVE-06`
- **Done when**:
  - [ ] `CulturaRepository.kt` atualizado:
    - Método `getCulturas()` renomeado para `getStoves()` retornando `Result<List<StoveResponse>>`
    - Chama `RetrofitClient.api.getStoves()` ao invés de `RetrofitClient.api.getCulturas()`
  - [ ] `HomeViewModel.kt` atualizado:
    - Usa `CulturaRepository.getStoves()` (ou cria `StoveRepository` existente)
    - `_culturas` renomeado para `_stoves` com tipo `MutableLiveData<List<StoveResponse>>`
    - Método `loadCulturas()` renomeado para `loadStoves()`
  - [ ] `CulturaAdapter.kt` atualizado:
    - Recebe `List<StoveResponse>` ao invés de `List<Cultura>`
    - `nome.text` exibe `stove.name` (nome da estufa dado pelo usuário)
    - Imagem usa drawable local baseado em `stove.crop`:
      - `"lettuce"` → `R.drawable.img_alface`
      - `"arugula"` → `R.drawable.img_rucula`
      - `"tomato"` → `R.drawable.img_tomate`
    - Remove dependência de Glide para carregar URL (usa `imagem.setImageResource(...)`)
  - [ ] `HomeActivity.kt` atualizado para usar os novos nomes (`loadStoves()`, etc.)
  - [ ] Gate check: Compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `refactor(home): adapt adapter, viewmodel and repository to StoveResponse model`

---

### T12: Atualizar `HomeActivity` para recarregar ao voltar e passar dados corretos ao `CulturaInfoActivity`
- **What**: Fazer a `HomeActivity` recarregar a lista de estufas quando o usuário volta da `CadastroEstufaActivity`, e passar `crop` (ao invés de `id` antigo) como extra para `CulturaInfoActivity`.
- **Where**: `app/src/main/java/com/example/esttufa/HomeActivity.kt`
- **Depends on**: T11
- **Reuses**: `HomeActivity.kt` existente
- **Requirement ID**: `MSTOVE-02`, `MSTOVE-03`, `MSTOVE-06`
- **Done when**:
  - [ ] FAB `fabAddEsttufa` agora usa `registerForActivityResult(StartActivityForResult)` para lançar `CadastroEstufaActivity`
  - [ ] No callback de resultado, se `resultCode == RESULT_OK`, chama `viewModel.loadStoves()` para recarregar
  - [ ] No `setOnItemClickListener` do `lvCulturas`:
    - Extra `"cultura"` agora envia `stove.crop` (ex: `"lettuce"`, `"tomato"`, `"arugula"`)
    - Opcionalmente envia `"stove_name"` como extra adicional para exibir o nome na tela de info
  - [ ] O `onResume()` também chama `viewModel.loadStoves()` para atualizar caso o usuário volte por back press
  - [ ] Personalizar `tvBoasVindas` para usar o displayName do Firebase: `FirebaseAuth.getInstance().currentUser?.displayName`
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(home): reload stoves on return, pass crop to CulturaInfoActivity`

---

### T13: Remover modelos e endpoints legados (Cultura, stoves/list)
- **What**: Remover os modelos `Cultura.kt`, `CulturaResponse.kt` e o endpoint `getCulturas()` do `ApiService` que não são mais utilizados após a migração para o novo modelo de dados.
- **Where**: `model/Cultura.kt`, `model/CulturaResponse.kt`, `model/ApiService.kt`, `repository/CulturaRepository.kt`
- **Depends on**: T12
- **Reuses**: None
- **Requirement ID**: `MMODEL-01`
- **Done when**:
  - [ ] Arquivo `model/Cultura.kt` deletado
  - [ ] Arquivo `model/CulturaResponse.kt` deletado
  - [ ] Endpoint `getCulturas()` removido de `ApiService.kt`
  - [ ] `CulturaRepository.kt` agora usa apenas `StoveResponse` / `StoveListResponse`
  - [ ] Gate check: `grep -r "Cultura" app/src/main/java/` retorna apenas referências ao `CulturaAdapter`, `CulturaInfoActivity` e `CulturaInfoViewModel` (que são nomes de UI, não do modelo legado)
  - [ ] Gate check: Projeto compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `refactor: remove legacy Cultura model and stoves/list endpoint`

---

### T14: Verificação final e smoke test
- **What**: Executar verificação completa end-to-end do fluxo de autenticação e CRUD de estufas contra a API de produção no Render.
- **Where**: Projeto completo
- **Depends on**: T13
- **Reuses**: None
- **Requirement ID**: Todos
- **Done when**:
  - [ ] Projeto compila sem erros: `./gradlew assembleDebug`
  - [ ] Tela de Login: login com email/senha válidos navega para Home
  - [ ] Tela de Login: login com credenciais inválidas exibe erro
  - [ ] Tela de Cadastro: registro com email novo cria conta e navega para Home
  - [ ] Tela de Cadastro: senhas diferentes exibe erro de validação
  - [ ] Auto-login: ao reabrir o app com sessão ativa, vai direto para Home
  - [ ] Home: lista de estufas carrega do `GET /stoves` com token Firebase
  - [ ] Home: empty state exibido quando não há estufas
  - [ ] Cadastro de Estufa: criar estufa via `POST /stoves` retorna 201 e volta para Home
  - [ ] Cadastro de Estufa: campo cultura com valor inválido retorna 422
  - [ ] Home: lista atualizada após criar nova estufa
  - [ ] CulturaInfo: clicar em uma estufa na lista navega para CulturaInfoActivity com dados corretos
  - [ ] Irrigação: `GET /irrigation-time/...` continua funcionando com token auth
  - [ ] Logs OkHttp: verificar que header `Authorization: Bearer <token>` está presente nas requisições
  - [ ] Nenhuma referência a `stoves/list`, `Cultura.kt` (model) ou `stoves_mock` permanece no código
- **Tests**: Manual smoke test completo contra `https://api-esttufa.onrender.com/`
- **Gate**: full
- **Commit**: nenhum (apenas verificação)
