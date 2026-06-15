# Planning Spec: Telas de Perfil do Usuário e Planos do Serviço
**Status**: DONE
**Started at**: 2026-06-14 15:53
**Completed at**: 2026-06-14 16:40
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
- **Languages & Runtime**: Kotlin, Java 11 target
- **Core Frameworks**: Android SDK (compileSdk 36, minSdk 34, targetSdk 36), Material Design 3, AndroidX
- **Networking**: Retrofit 2.9.0 + Gson Converter + OkHttp 4.11.0 (LoggingInterceptor)
- **Async**: Kotlin Coroutines 1.7.3 + ViewModel KTX (lifecycle-viewmodel-ktx 2.10.0)
- **Firebase**: BOM 34.14.1, firebase-auth, play-services-auth
- **Build**: View Binding habilitado. Sem Compose, sem Navigation Component, sem Hilt/Dagger.
- **API de produção**: `https://api-esttufa.onrender.com/` (Render)
- **Observed Conventions & Patterns**:
  - MVVM: Activity → ViewModel → Repository → RetrofitClient (singleton) → ApiService (Retrofit)
  - ViewModels usam LiveData + `viewModelScope` com coroutines
  - Repositories retornam `Result<T>` via `runCatching`
  - Navegação entre telas via `Intent` explícito (sem Navigation Component)
  - View Binding para todos os layouts
  - Activities com `configChanges="orientation|screenSize|screenLayout"`
  - Pacote base: `com.example.esttufa`
  - Fontes: Poppins (bold, medium) para títulos, Inter Medium para subtítulos, Nunito para corpo
  - Cores: primary `#059162`, secondary `#1C7054`, tertiary/accent `#FF793F`
  - Tema: `Theme.Material3.DayNight.NoActionBar`
  - Sem DI — repositories instanciados diretamente nos ViewModels
- **Risks & Fragile Areas**:
  - Não existe endpoint de perfil ou planos na API atual — dados do usuário vêm exclusivamente do Firebase Auth (`displayName`, `email`, `photoUrl`)
  - A API não retorna dados de assinatura; estes serão mockados localmente para esta iteração
  - Singleton do RetrofitClient dificulta injeção em testes

---

## 2. Requirements & Acceptance Criteria
- **Goal**: Criar duas novas telas — Perfil do Usuário e Planos do Serviço — acessíveis a partir da Home existente, seguindo o fluxo Home → Perfil → Planos. A tela de Perfil exibe dados do produtor e atalhos de configuração. A tela de Planos exibe os 5 planos do serviço Esttufa com opção de assinar.

### Acceptance Criteria

#### Perfil
- `PERF-01`: WHEN o usuário toca no avatar ou cabeçalho na `HomeActivity` THEN o sistema SHALL abrir a `ProfileActivity` com os dados do usuário carregados.
- `PERF-02`: WHEN a `ProfileActivity` é exibida THEN o sistema SHALL mostrar avatar circular com iniciais do nome (ou foto se disponível), nome completo, e badge do plano atual (ex: "Plano Muda").
- `PERF-03`: WHEN a seção "Informações pessoais" é exibida THEN o sistema SHALL mostrar: nome, e-mail, telefone e localização, cada qual com ícone de edição. Campos sem valor devem exibir "Não informado" em cor secundária.
- `PERF-04`: WHEN a seção "Minha assinatura" é exibida THEN o sistema SHALL mostrar o nome do plano atual, data de renovação e um botão "Ver todos os planos".
- `PERF-05`: WHEN o usuário toca em "Ver todos os planos" THEN o sistema SHALL navegar para a `PlansActivity`.
- `PERF-06`: WHEN o usuário toca em "Sair" na seção de configurações THEN o sistema SHALL exibir um `MaterialAlertDialog` de confirmação. Se confirmado, SHALL fazer signOut do Firebase e navegar para `MainActivity` limpando a back stack.
- `PERF-07`: WHEN a seção de configurações é exibida THEN o sistema SHALL renderizar itens com ícone à esquerda, label centralizado e chevron à direita para: Notificações, Idioma, Suporte, Termos de uso, Sair.
- `PERF-08`: WHEN o usuário pressiona o botão de voltar (hardware ou toolbar) THEN o sistema SHALL retornar à `HomeActivity`.
- `PERF-09`: WHEN o perfil está carregando THEN o sistema SHALL exibir um `ProgressBar` centralizado e ocultar o conteúdo.
- `PERF-10`: WHEN ocorre um erro ao carregar dados THEN o sistema SHALL exibir mensagem de erro com botão "Tentar novamente".

#### Planos
- `PLAN-01`: WHEN a `PlansActivity` é aberta THEN o sistema SHALL exibir uma lista vertical scrollável com os 5 cards de planos.
- `PLAN-02`: WHEN cada card é renderizado THEN o sistema SHALL mostrar: nome do plano, faixa de preço, até 3 benefícios visíveis com link "ver mais", e botão de ação.
- `PLAN-03`: WHEN o plano exibido é o plano atual do usuário THEN o botão SHALL mostrar "Plano atual" em estado desabilitado (não clicável, cor esmaecida).
- `PLAN-04`: WHEN o plano NÃO é o atual THEN o botão SHALL mostrar "Assinar" em cor `tertiary` (#FF793F) e ser clicável.
- `PLAN-05`: WHEN o plano é recomendado (Muda) THEN o card SHALL exibir badge "Mais popular" no topo e ter borda de destaque em cor `tertiary`.
- `PLAN-06`: WHEN o usuário toca em "Assinar" THEN o sistema SHALL exibir um `BottomSheetDialogFragment` com resumo do plano (nome, preço, top 3 benefícios) e botão "Confirmar assinatura".
- `PLAN-07`: WHEN o usuário toca em "Confirmar assinatura" no bottom sheet THEN o sistema SHALL atualizar o plano local do usuário, fechar o bottom sheet, e exibir um `Snackbar` de confirmação.
- `PLAN-08`: WHEN o usuário toca em "ver mais" em um card THEN o card SHALL expandir para mostrar todos os benefícios com animação suave.
- `PLAN-09`: WHEN a lista de planos está carregando THEN o sistema SHALL exibir shimmer/placeholder skeleton.
- `PLAN-10`: WHEN ocorre erro ao carregar planos THEN o sistema SHALL exibir estado de erro com ícone, mensagem e botão "Tentar novamente".
- `PLAN-11`: WHEN o usuário pressiona voltar THEN o sistema SHALL retornar à `ProfileActivity`.

#### Acessibilidade
- `ACC-01`: WHEN qualquer ícone decorativo é renderizado THEN ele SHALL ter `contentDescription` descritivo em português.
- `ACC-02`: WHEN textos são exibidos THEN eles SHALL respeitar a escala de fonte do sistema (`sp` para text sizes).
- `ACC-03`: WHEN o badge de plano é exibido THEN ele SHALL ter `contentDescription` com o nome do plano para leitores de tela.

---

## 3. Design & Impact Analysis

### Code Reuse Blueprints
- **Activity Pattern**: `HomeActivity.kt` — padrão de Activity com View Binding, ViewModel e observação de LiveData.
- **ViewModel Pattern**: `HomeViewModel.kt` — padrão de ViewModel com `MutableLiveData`, `viewModelScope.launch`, e estados Loading/Data/Empty.
- **Repository Pattern**: `StoveRepository.kt` — padrão de Repository com `ApiService` injetável e `runCatching`.
- **Adapter Pattern**: `CulturaAdapter.kt` — padrão de ArrayAdapter com inflate de item layout.
- **Logout Pattern**: `UnauthorizedSessionHandler.kt` — signOut Firebase + clear task stack via Intent flags.
- **Layout Pattern**: `activity_home.xml` — ConstraintLayout com seções, ProgressBar, estado vazio.

### Wireframe Descritivo — Tela de Perfil (`activity_profile.xml`)

```
┌──────────────────────────────────────────┐
│  ← Meu Perfil                            │  ← Toolbar com botão de voltar
├──────────────────────────────────────────┤
│  ┌──────┐                                │
│  │Avatar│  João Silva                    │  Avatar circular 72dp
│  │ "JS" │  🏷 Plano Muda                │  Badge de plano
│  └──────┘                                │
├──────────────────────────────────────────┤
│  INFORMAÇÕES PESSOAIS          ✏ Editar  │  Section header
│  ┌──────────────────────────────────────┐│
│  │ 👤 Nome        João Silva         ✏ ││  Info row
│  │ 📧 E-mail      joao@email.com     ✏ ││
│  │ 📱 Telefone    (11) 99999-9999    ✏ ││
│  │ 📍 Localização Campinas - SP      ✏ ││
│  └──────────────────────────────────────┘│
├──────────────────────────────────────────┤
│  MINHA ASSINATURA                        │
│  ┌──────────────────────────────────────┐│
│  │ Plano Muda                          ││
│  │ Renovação: 15/07/2026               ││
│  │ [    Ver todos os planos    ]       ││  Botão outlined
│  └──────────────────────────────────────┘│
├──────────────────────────────────────────┤
│  CONFIGURAÇÕES                           │
│  ┌──────────────────────────────────────┐│
│  │ 🔔  Notificações              ›     ││  Setting item row
│  │ 🌐  Idioma                    ›     ││
│  │ 💬  Suporte                   ›     ││
│  │ 📄  Termos de uso             ›     ││
│  │ 🚪  Sair                      ›     ││  Cor vermelha/destaque
│  └──────────────────────────────────────┘│
└──────────────────────────────────────────┘
```

### Wireframe Descritivo — Tela de Planos (`activity_plans.xml`)

```
┌──────────────────────────────────────────┐
│  ← Planos Esttufa                        │  Toolbar
├──────────────────────────────────────────┤
│  Escolha o plano ideal para              │
│  sua produção 🌱                         │  Subtítulo
├──────────────── ScrollView ──────────────┤
│  ┌──────────────────────────────────────┐│
│  │ 🌱 Plano Semente                    ││  Card sem destaque
│  │ R$ 199–300/mês                      ││
│  │ • Irrigação automatizada básica     ││
│  │ • Até 2 estufas                     ││
│  │ [       Assinar       ]             ││  Botão filled tertiary
│  └──────────────────────────────────────┘│
│                                          │
│  ┌══════════════════════════════════════┐│
│  ║ ⭐ MAIS POPULAR                     ║│  Card com borda destaque
│  ║ 🌿 Plano Muda                      ║│  (tertiary border + badge)
│  ║ R$ 399,90–599,90/mês               ║│
│  ║ • Tudo do Semente + visão comp.    ║│
│  ║ • Detecção de pragas               ║│
│  ║ • Até 4 estufas                    ║│
│  ║    ver mais ▼                       ║│
│  ║ [ Plano atual ] (desabilitado)     ║│  Se for plano do user
│  ╚══════════════════════════════════════╝│
│                                          │
│  ┌──────────────────────────────────────┐│
│  │ 🌾 Plano Colheita                  ││
│  │ Consultar valor                     ││
│  │ • Tudo do Muda + previsão           ││
│  │ • Controle ventiladores/holofotes   ││
│  │ • Controle luminosidade/umidade     ││
│  │ [       Assinar       ]             ││
│  └──────────────────────────────────────┘│
│                                          │
│  ┌──────────────────────────────────────┐│
│  │ 🏆 Plano Safra                     ││
│  │ R$ 1.599,90–2.599,90/mês           ││
│  │ • Tudo do Colheita + técnico agr.   ││
│  │ • Adubação, aquecimento, pestic.    ││
│  │ • Previsão de lucro                 ││
│  │    ver mais ▼                       ││
│  │ [       Assinar       ]             ││
│  └──────────────────────────────────────┘│
│                                          │
│  ┌──────────────────────────────────────┐│
│  │ 🚜 Plano Colheitadeira             ││
│  │ A partir de R$ 15.000/mês          ││
│  │ • Plano empresarial completo        ││
│  │ • Visitas imediatas + manutenção    ││
│  │ • Até 6 estufas + expansão          ││
│  │    ver mais ▼                       ││
│  │ [       Assinar       ]             ││
│  └──────────────────────────────────────┘│
└──────────────────────────────────────────┘

Bottom Sheet (ao clicar "Assinar"):
┌──────────────────────────────────────────┐
│  ━━━━ (drag handle)                      │
│  Confirmar Assinatura                    │
│                                          │
│  🌾 Plano Colheita                      │
│  Consultar valor                         │
│                                          │
│  ✓ Tudo do Muda + previsão               │
│  ✓ Controle ventiladores/holofotes       │
│  ✓ Controle luminosidade/umidade         │
│                                          │
│  [    Confirmar assinatura    ]          │  Botão filled tertiary
│  [         Cancelar           ]          │  Botão text
└──────────────────────────────────────────┘
```

### Modelo de Dados (data classes Kotlin)

```kotlin
// --- Perfil do Usuário ---
// Arquivo: com/example/esttufa/model/UserProfile.kt
data class UserProfile(
    val displayName: String,
    val email: String,
    val photoUrl: String?,      // URL do Firebase Auth
    val phone: String?,         // Mock local — Firebase Auth não armazena
    val location: String?,      // Mock local — não existe na API
    val currentPlanId: String,  // ID do plano ativo
    val renewalDate: String?    // Data de renovação (mock: "15/07/2026")
)

// --- Plano do Serviço ---
// Arquivo: com/example/esttufa/model/Plan.kt
data class Plan(
    val id: String,             // Ex: "semente", "muda", "colheita", "safra", "colheitadeira"
    val name: String,           // Ex: "Plano Semente"
    val priceRange: String,     // Ex: "R$ 199–300/mês" ou "Consultar valor"
    val benefits: List<String>, // Lista completa de benefícios
    val maxVisibleBenefits: Int = 3, // Quantos mostrar antes de "ver mais"
    val isRecommended: Boolean, // true para Plano Muda
    val emoji: String           // Emoji decorativo: "🌱", "🌿", "🌾", "🏆", "🚜"
)

// --- Item de configuração do perfil ---
// Arquivo: com/example/esttufa/model/SettingsItem.kt
data class SettingsItem(
    val id: String,             // Ex: "notifications", "language", "support", "terms", "logout"
    val iconResId: Int,         // @drawable resource ID
    val label: String,          // Texto visível
    val isDestructive: Boolean = false // true para "Sair" (cor vermelha)
)
```

### Estrutura de ViewModel e Estados de UI

```kotlin
// --- ProfileViewModel ---
// Arquivo: com/example/esttufa/viewmodel/ProfileViewModel.kt
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableLiveData<ProfileUiState>()
    val uiState: LiveData<ProfileUiState> = _uiState

    fun loadProfile() { /* ... */ }
    fun logout() { /* FirebaseAuth.signOut() */ }
}

// --- PlansViewModel ---
// Arquivo: com/example/esttufa/viewmodel/PlansViewModel.kt
sealed class PlansUiState {
    object Loading : PlansUiState()
    data class Success(
        val plans: List<Plan>,
        val currentPlanId: String
    ) : PlansUiState()
    data class Error(val message: String) : PlansUiState()
}

sealed class SubscriptionEvent {
    data class ConfirmSubscription(val plan: Plan) : SubscriptionEvent()
    data class SubscriptionSuccess(val planName: String) : SubscriptionEvent()
    data class SubscriptionError(val message: String) : SubscriptionEvent()
}

class PlansViewModel : ViewModel() {
    private val _uiState = MutableLiveData<PlansUiState>()
    val uiState: LiveData<PlansUiState> = _uiState

    private val _subscriptionEvent = MutableLiveData<SubscriptionEvent?>()
    val subscriptionEvent: LiveData<SubscriptionEvent?> = _subscriptionEvent

    fun loadPlans() { /* ... */ }
    fun subscribeToPlan(planId: String) { /* ... */ }
    fun clearEvent() { _subscriptionEvent.value = null }
}
```

### Rotas de Navegação

| Origem | Destino | Trigger | Intent Extras |
|---|---|---|---|
| `HomeActivity` | `ProfileActivity` | Toque no avatar/cabeçalho | Nenhum (dados vêm do Firebase/local) |
| `ProfileActivity` | `PlansActivity` | Botão "Ver todos os planos" | Nenhum |
| `ProfileActivity` | `MainActivity` | Confirmar logout | `FLAG_ACTIVITY_NEW_TASK \| FLAG_ACTIVITY_CLEAR_TASK` |
| `PlansActivity` | `ProfileActivity` | Botão voltar (hardware/toolbar) | `finish()` |
| `ProfileActivity` | `HomeActivity` | Botão voltar (hardware/toolbar) | `finish()` |

### Casos de Borda e Estados Vazios

| Cenário | Comportamento Esperado |
|---|---|
| Usuário sem `displayName` no Firebase | Exibir "Usuário" como nome e "U" como iniciais do avatar |
| Usuário sem `email` | Exibir "Não informado" em cor `text_secondary_light` |
| Usuário sem foto (`photoUrl == null`) | Mostrar avatar com iniciais (2 primeiros caracteres das primeiras 2 palavras) sobre fundo `primary` |
| Usuário sem plano ativo (`currentPlanId` vazio) | Exibir "Sem plano ativo" na seção de assinatura, ocultar data de renovação |
| Erro ao carregar perfil | Tela de erro com ícone, mensagem e botão "Tentar novamente" |
| Lista de planos vazia (não deveria ocorrer — dados hardcoded) | Exibir estado vazio genérico com mensagem "Nenhum plano disponível" |
| Usuário já possui o plano selecionado | Botão "Plano atual" desabilitado, não abre bottom sheet |
| Rotação de tela | ViewModel mantém estado via LiveData; layout reconstrói a partir do estado salvo |

### Files to Modify

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | Registrar `ProfileActivity` e `PlansActivity` |
| `app/src/main/java/com/example/esttufa/HomeActivity.kt` | Adicionar listener de clique no avatar/cabeçalho para abrir `ProfileActivity` |
| `app/src/main/res/layout/activity_home.xml` | Tornar o cabeçalho (llHeader ou ivAvatar) clicável |
| `app/src/main/res/values/strings.xml` | Adicionar strings das novas telas |
| `app/src/main/res/values/colors.xml` | Adicionar cores: `badge_plan_background`, `error_red`, `disabled_button` |

### New Files to Create

| File | Purpose |
|---|---|
| `app/src/main/java/com/example/esttufa/ProfileActivity.kt` | Activity da tela de Perfil |
| `app/src/main/java/com/example/esttufa/PlansActivity.kt` | Activity da tela de Planos |
| `app/src/main/java/com/example/esttufa/viewmodel/ProfileViewModel.kt` | ViewModel do Perfil |
| `app/src/main/java/com/example/esttufa/viewmodel/PlansViewModel.kt` | ViewModel dos Planos |
| `app/src/main/java/com/example/esttufa/model/UserProfile.kt` | Data class do perfil do usuário |
| `app/src/main/java/com/example/esttufa/model/Plan.kt` | Data class do plano |
| `app/src/main/java/com/example/esttufa/model/SettingsItem.kt` | Data class de item de configuração |
| `app/src/main/java/com/example/esttufa/repository/UserRepository.kt` | Repository de dados do usuário (Firebase Auth + mock local) |
| `app/src/main/java/com/example/esttufa/repository/PlanRepository.kt` | Repository de planos (dados hardcoded nesta iteração) |
| `app/src/main/java/com/example/esttufa/adapter/SettingsAdapter.kt` | Adapter para lista de configurações |
| `app/src/main/java/com/example/esttufa/adapter/PlanAdapter.kt` | Adapter (RecyclerView) para lista de planos |
| `app/src/main/java/com/example/esttufa/ConfirmSubscriptionBottomSheet.kt` | BottomSheetDialogFragment de confirmação |
| `app/src/main/res/layout/activity_profile.xml` | Layout XML do Perfil |
| `app/src/main/res/layout/activity_plans.xml` | Layout XML dos Planos |
| `app/src/main/res/layout/item_setting.xml` | Layout do item de configuração |
| `app/src/main/res/layout/item_plan_card.xml` | Layout do card de plano |
| `app/src/main/res/layout/bottom_sheet_confirm_subscription.xml` | Layout do bottom sheet |
| `app/src/main/res/drawable/ic_notifications.xml` | Ícone de notificações (Material icon) |
| `app/src/main/res/drawable/ic_language.xml` | Ícone de idioma |
| `app/src/main/res/drawable/ic_support.xml` | Ícone de suporte |
| `app/src/main/res/drawable/ic_terms.xml` | Ícone de termos |
| `app/src/main/res/drawable/ic_logout.xml` | Ícone de sair |
| `app/src/main/res/drawable/ic_edit.xml` | Ícone de edição |
| `app/src/main/res/drawable/ic_chevron_right.xml` | Ícone de chevron |
| `app/src/main/res/drawable/bg_badge_plan.xml` | Background shape do badge de plano |
| `app/src/main/res/drawable/bg_avatar_initials.xml` | Background circular para avatar com iniciais |
| `app/src/main/res/drawable/bg_plan_card_highlighted.xml` | Background com borda de destaque para card de plano recomendado |

---

## 4. Execution Plan & Parallelism Map

```
Phase 1 (Sequential — Fundação de dados):
  T1 (Models) ──→ T2 (Repositories)

Phase 2 (Parallel — ViewModels):
  T2 complete, then:
    ├── T3 (ProfileViewModel) [P]
    └── T4 (PlansViewModel) [P]

Phase 3 (Parallel — Layouts e Drawables):
  T2 complete, then:
    ├── T5 (Drawables + Strings + Colors) [P]
    ├── T6 (activity_profile.xml + item_setting.xml) [P]
    └── T7 (activity_plans.xml + item_plan_card.xml + bottom_sheet layout) [P]

Phase 4 (Parallel — Adapters):
  T5, T6, T7 complete, then:
    ├── T8 (SettingsAdapter) [P]
    └── T9 (PlanAdapter) [P]

Phase 5 (Sequential — Activities):
  T3, T4, T8, T9 complete, then:
    T10 (ProfileActivity) ──→ T11 (PlansActivity + BottomSheet)

Phase 6 (Sequential — Integração com Home):
  T10, T11 complete, then:
    T12 (Conectar HomeActivity + AndroidManifest)

Phase 7 (Sequential — Verificação final):
  T12 complete, then:
    T13 (Build + Smoke test)
```

> [!IMPORTANT]
> **Subagent Concurrency Rules**: For each task marked with `[P]`, the executing agent **MUST spawn a separate concurrent subagent** to perform the work. Sequential tasks (without `[P]`) should be executed inline or by single sequential subagents.

---

## 5. Granular Task Checklist

### T1: Criar Data Classes do Modelo (UserProfile, Plan, SettingsItem)
- **What**: Criar os 3 arquivos de modelo de dados necessários para as telas de Perfil e Planos.
- **Where**:
  - `app/src/main/java/com/example/esttufa/model/UserProfile.kt`
  - `app/src/main/java/com/example/esttufa/model/Plan.kt`
  - `app/src/main/java/com/example/esttufa/model/SettingsItem.kt`
- **Depends on**: None
- **Reuses**: Padrão de data classes existente (`StoveResponse.kt`)
- **Requirement ID**: `PERF-02`, `PERF-03`, `PERF-04`, `PERF-07`, `PLAN-02`
- **Done when**:
  - [ ] `UserProfile.kt` criado com campos: `displayName`, `email`, `photoUrl`, `phone`, `location`, `currentPlanId`, `renewalDate`
  - [ ] `Plan.kt` criado com campos: `id`, `name`, `priceRange`, `benefits: List<String>`, `maxVisibleBenefits`, `isRecommended`, `emoji`
  - [ ] `SettingsItem.kt` criado com campos: `id`, `iconResId: Int`, `label`, `isDestructive: Boolean`
  - [ ] Todos os arquivos no pacote `com.example.esttufa.model`
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none (data classes puras)
- **Gate**: quick
- **Commit**: `feat(model): add UserProfile, Plan, and SettingsItem data classes`

---

### T2: Criar Repositories (UserRepository, PlanRepository)
- **What**: Criar os 2 repositories que fornecem dados para as novas telas. `UserRepository` busca dados do Firebase Auth e mock local. `PlanRepository` fornece lista hardcoded dos 5 planos.
- **Where**:
  - `app/src/main/java/com/example/esttufa/repository/UserRepository.kt`
  - `app/src/main/java/com/example/esttufa/repository/PlanRepository.kt`
- **Depends on**: T1
- **Reuses**: `StoveRepository.kt` (padrão de `Result<T>` com `runCatching`)
- **Requirement ID**: `PERF-02`, `PERF-03`, `PERF-04`, `PLAN-01`
- **Done when**:
  - [ ] `UserRepository.kt` criado com:
    - `fun getProfile(): Result<UserProfile>` — busca `FirebaseAuth.currentUser` e monta `UserProfile` com dados mock para campos não disponíveis (phone, location, currentPlanId, renewalDate)
    - `fun logout()` — chama `FirebaseAuth.getInstance().signOut()`
  - [ ] `PlanRepository.kt` criado com:
    - `fun getPlans(): Result<List<Plan>>` — retorna lista hardcoded dos 5 planos:
      1. **Semente** (id="semente", "R$ 199–300/mês", emoji="🌱"): Irrigação automatizada básica (temperatura + tempo); Até 2 estufas
      2. **Muda** (id="muda", "R$ 399,90–599,90/mês", isRecommended=true, emoji="🌿"): Tudo do Semente + visão computacional das plantas; Detecção de pragas e saúde das folhas; Até 4 estufas
      3. **Colheita** (id="colheita", "Consultar valor", emoji="🌾"): Tudo do Muda + previsão de crescimento e pragas; Controle de ventiladores, holofotes e lonas; Controle de luminosidade e umidade do ar
      4. **Safra** (id="safra", "R$ 1.599,90–2.599,90/mês", emoji="🏆"): Tudo do Colheita + visitas quinzenais de técnico agrônomo; Controle automático de adubação, aquecimento e pesticidas; Previsão de lucro e detecção de sazonalidade
      5. **Colheitadeira** (id="colheitadeira", "A partir de R$ 15.000/mês", emoji="🚜"): Plano empresarial completo; Visitas imediatas, manutenção semanal; Até 6 estufas + expansão mediante adicional; Testagem genética e sugestão de cruzamento de hortaliças; 1 consultoria gratuita por mês
    - `fun getUserCurrentPlanId(): String` — retorna `"muda"` (mock)
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none (repositórios com dados mock nesta iteração)
- **Gate**: quick
- **Commit**: `feat(repository): add UserRepository and PlanRepository with mock data`

---

### T3: Criar ProfileViewModel [P]
- **What**: Criar o ViewModel da tela de Perfil com sealed class de estados e ações de load/logout.
- **Where**: `app/src/main/java/com/example/esttufa/viewmodel/ProfileViewModel.kt`
- **Depends on**: T1, T2
- **Reuses**: `HomeViewModel.kt` (padrão LiveData + viewModelScope)
- **Requirement ID**: `PERF-02`, `PERF-06`, `PERF-09`, `PERF-10`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] `ProfileViewModel.kt` criado com:
    - `sealed class ProfileUiState` com `Loading`, `Success(profile: UserProfile)`, `Error(message: String)`
    - `val uiState: LiveData<ProfileUiState>` publicando estados
    - `fun loadProfile()` — usa `UserRepository` para obter perfil, publica `Loading` → `Success`/`Error`
    - `fun logout()` — chama `UserRepository.logout()`
    - Propriedade `val settingsItems: List<SettingsItem>` — lista fixa dos 5 itens de configuração
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none (ViewModel leve sem lógica complexa nesta iteração)
- **Gate**: quick
- **Commit**: `feat(viewmodel): add ProfileViewModel with UI states and logout`

---

### T4: Criar PlansViewModel [P]
- **What**: Criar o ViewModel da tela de Planos com sealed class de estados, carregamento e ação de assinatura.
- **Where**: `app/src/main/java/com/example/esttufa/viewmodel/PlansViewModel.kt`
- **Depends on**: T1, T2
- **Reuses**: `HomeViewModel.kt` (padrão LiveData + viewModelScope)
- **Requirement ID**: `PLAN-01`, `PLAN-06`, `PLAN-07`, `PLAN-09`, `PLAN-10`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] `PlansViewModel.kt` criado com:
    - `sealed class PlansUiState` com `Loading`, `Success(plans: List<Plan>, currentPlanId: String)`, `Error(message: String)`
    - `sealed class SubscriptionEvent` com `ConfirmSubscription(plan: Plan)`, `SubscriptionSuccess(planName: String)`, `SubscriptionError(message: String)`
    - `val uiState: LiveData<PlansUiState>` publicando estados
    - `val subscriptionEvent: LiveData<SubscriptionEvent?>` — evento one-shot
    - `fun loadPlans()` — usa `PlanRepository` para obter lista e plano atual
    - `fun requestSubscription(plan: Plan)` — emite `ConfirmSubscription`
    - `fun confirmSubscription(planId: String)` — atualiza plano local, emite `SubscriptionSuccess`
    - `fun clearEvent()` — limpa evento após consumir
    - `MutableMap<String, Boolean>` para rastrear quais cards estão expandidos
    - `fun toggleCardExpansion(planId: String)` — inverte estado de expansão
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(viewmodel): add PlansViewModel with subscription events`

---

### T5: Criar Drawables, Strings e Colors das Novas Telas [P]
- **What**: Criar todos os recursos visuais (ícones Material vector, shapes, strings, cores) necessários para as duas telas.
- **Where**:
  - `app/src/main/res/drawable/ic_notifications.xml`
  - `app/src/main/res/drawable/ic_language.xml`
  - `app/src/main/res/drawable/ic_support.xml`
  - `app/src/main/res/drawable/ic_terms.xml`
  - `app/src/main/res/drawable/ic_logout.xml`
  - `app/src/main/res/drawable/ic_edit.xml`
  - `app/src/main/res/drawable/ic_chevron_right.xml`
  - `app/src/main/res/drawable/bg_badge_plan.xml`
  - `app/src/main/res/drawable/bg_avatar_initials.xml`
  - `app/src/main/res/drawable/bg_plan_card_highlighted.xml`
  - `app/src/main/res/values/strings.xml` (adicionar novas strings)
  - `app/src/main/res/values/colors.xml` (adicionar novas cores)
- **Depends on**: T1
- **Reuses**: Drawables existentes como `ic_arrow_back.xml` para referência de estilo de vector drawable.
- **Requirement ID**: `PERF-07`, `PLAN-05`, `ACC-01`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] 7 ícones vector drawable criados (notifications, language, support, terms, logout, edit, chevron_right) — todos em 24dp, pathData Material Design
  - [ ] `bg_badge_plan.xml` criado — shape retangular com corners 12dp, fundo `primary` com alpha
  - [ ] `bg_avatar_initials.xml` criado — oval/circle com fundo `primary`
  - [ ] `bg_plan_card_highlighted.xml` criado — shape retangular com corners 16dp, stroke 2dp cor `tertiary`
  - [ ] `strings.xml` atualizado com ~30 novas strings para Perfil e Planos (todas em pt-BR)
  - [ ] `colors.xml` atualizado com: `badge_plan_background (#E8F5E9)`, `error_red (#D32F2F)`, `disabled_button (#BDBDBD)`, `divider_color (#E0E0E0)`
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none (recursos visuais)
- **Gate**: quick
- **Commit**: `feat(resources): add drawables, strings and colors for Profile and Plans screens`

**Detalhamento das novas strings a adicionar em `strings.xml`:**

```xml
<!-- Tela de Perfil -->
<string name="profile_title">Meu Perfil</string>
<string name="profile_section_info">Informações pessoais</string>
<string name="profile_section_subscription">Minha assinatura</string>
<string name="profile_section_settings">Configurações</string>
<string name="profile_label_name">Nome</string>
<string name="profile_label_email">E-mail</string>
<string name="profile_label_phone">Telefone</string>
<string name="profile_label_location">Localização</string>
<string name="profile_not_informed">Não informado</string>
<string name="profile_no_plan">Sem plano ativo</string>
<string name="profile_renewal_label">Renovação: %s</string>
<string name="profile_view_plans">Ver todos os planos</string>
<string name="profile_edit_description">Editar campo</string>
<string name="profile_avatar_description">Foto do perfil</string>
<string name="profile_plan_badge_description">Plano atual: %s</string>

<!-- Configurações -->
<string name="settings_notifications">Notificações</string>
<string name="settings_language">Idioma</string>
<string name="settings_support">Suporte</string>
<string name="settings_terms">Termos de uso</string>
<string name="settings_logout">Sair</string>
<string name="settings_chevron_description">Abrir %s</string>

<!-- Diálogo de logout -->
<string name="logout_dialog_title">Sair do Esttufa</string>
<string name="logout_dialog_message">Tem certeza que deseja sair da sua conta?</string>
<string name="logout_dialog_confirm">Sair</string>
<string name="logout_dialog_cancel">Cancelar</string>

<!-- Tela de Planos -->
<string name="plans_title">Planos Esttufa</string>
<string name="plans_subtitle">Escolha o plano ideal para sua produção 🌱</string>
<string name="plans_badge_popular">Mais popular</string>
<string name="plans_btn_subscribe">Assinar</string>
<string name="plans_btn_current">Plano atual</string>
<string name="plans_see_more">ver mais</string>
<string name="plans_see_less">ver menos</string>
<string name="plans_empty">Nenhum plano disponível</string>
<string name="plans_error_retry">Tentar novamente</string>

<!-- Bottom Sheet de Confirmação -->
<string name="subscription_confirm_title">Confirmar Assinatura</string>
<string name="subscription_confirm_btn">Confirmar assinatura</string>
<string name="subscription_cancel_btn">Cancelar</string>
<string name="subscription_success_message">Assinatura do %s realizada com sucesso!</string>

<!-- Erro genérico -->
<string name="error_loading_profile">Erro ao carregar perfil. Tente novamente.</string>
<string name="error_loading_plans">Erro ao carregar planos. Tente novamente.</string>
<string name="btn_retry">Tentar novamente</string>
```

---

### T6: Criar Layout XML da Tela de Perfil [P]
- **What**: Criar os layouts XML da tela de Perfil: `activity_profile.xml` e `item_setting.xml`.
- **Where**:
  - `app/src/main/res/layout/activity_profile.xml`
  - `app/src/main/res/layout/item_setting.xml`
- **Depends on**: T1, T5 (drawables e strings disponíveis)
- **Reuses**: `activity_home.xml` (padrão ConstraintLayout com header + conteúdo)
- **Requirement ID**: `PERF-02`, `PERF-03`, `PERF-04`, `PERF-07`, `ACC-01`, `ACC-02`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] `activity_profile.xml` criado com:
    - `ScrollView` raiz envolvendo `LinearLayout` vertical
    - Toolbar com botão voltar e título "Meu Perfil"
    - Seção de cabeçalho: `ShapeableImageView` circular 72dp (avatar), `TextView` nome, `TextView` badge plano com background `bg_badge_plan`
    - Seção "Informações pessoais": 4 rows (nome, email, telefone, localização) cada com ícone, label, valor e botão editar
    - Seção "Minha assinatura": card com plano atual, data renovação, botão outlined "Ver todos os planos"
    - Seção "Configurações": `ListView` ou `LinearLayout` com items (ícone + label + chevron)
    - `ProgressBar` centralizado (visibilidade controlada pelo ViewModel)
    - Error state: `LinearLayout` com ícone, mensagem e botão retry
    - Todos os `textSize` em `sp`, paddings em `dp`
    - `contentDescription` em todos os ícones e elementos interativos
  - [ ] `item_setting.xml` criado com:
    - Layout horizontal: ícone 24dp à esquerda, `TextView` label com weight, `ImageView` chevron 16dp à direita
    - Padding vertical 16dp, horizontal 24dp
    - `android:background="?attr/selectableItemBackground"` para ripple
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none (layout XML)
- **Gate**: quick
- **Commit**: `feat(layout): add activity_profile.xml and item_setting.xml`

---

### T7: Criar Layout XML da Tela de Planos e Bottom Sheet [P]
- **What**: Criar os layouts XML da tela de Planos: `activity_plans.xml`, `item_plan_card.xml` e `bottom_sheet_confirm_subscription.xml`.
- **Where**:
  - `app/src/main/res/layout/activity_plans.xml`
  - `app/src/main/res/layout/item_plan_card.xml`
  - `app/src/main/res/layout/bottom_sheet_confirm_subscription.xml`
- **Depends on**: T1, T5 (drawables e strings disponíveis)
- **Reuses**: `activity_home.xml` (padrão de layout com toolbar e lista)
- **Requirement ID**: `PLAN-01`, `PLAN-02`, `PLAN-05`, `PLAN-06`, `ACC-01`, `ACC-02`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] `activity_plans.xml` criado com:
    - `ConstraintLayout` raiz
    - Toolbar com botão voltar e título "Planos Esttufa"
    - `TextView` subtítulo ("Escolha o plano ideal...")
    - `RecyclerView` vertical para os cards (id `rvPlans`)
    - `ProgressBar` centralizado para loading
    - Error state layout (ícone + mensagem + botão retry)
    - Empty state layout
  - [ ] `item_plan_card.xml` criado com:
    - `MaterialCardView` com corners 16dp e elevation 2dp
    - Badge "Mais popular" (`TextView` com `bg_badge_plan`, visibilidade `GONE` por default)
    - Emoji + nome do plano em `TextStyle.Title`
    - Preço em `TextStyle.Subtitle`
    - `LinearLayout` vertical para benefícios (bullets "•")
    - `TextView` "ver mais" clicável (cor `primary`)
    - `MaterialButton` de ação (Assinar / Plano atual)
    - Card recomendado usa stroke `tertiary` 2dp
  - [ ] `bottom_sheet_confirm_subscription.xml` criado com:
    - Drag handle no topo
    - Título "Confirmar Assinatura"
    - Nome do plano e preço
    - Lista de até 3 benefícios com ícone de check
    - Botão filled "Confirmar assinatura" (cor `tertiary`)
    - Botão text "Cancelar"
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none (layout XML)
- **Gate**: quick
- **Commit**: `feat(layout): add activity_plans.xml, item_plan_card.xml and bottom sheet layout`

---

### T8: Criar SettingsAdapter [P]
- **What**: Criar adapter para a lista de itens de configuração na tela de Perfil.
- **Where**: `app/src/main/java/com/example/esttufa/adapter/SettingsAdapter.kt`
- **Depends on**: T1, T5, T6
- **Reuses**: `CulturaAdapter.kt` (padrão de ArrayAdapter com inflate)
- **Requirement ID**: `PERF-07`, `ACC-01`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] `SettingsAdapter.kt` criado como `ArrayAdapter<SettingsItem>` com:
    - Inflate de `item_setting.xml`
    - Bind: ícone via `setImageResource(item.iconResId)`, label via `setText(item.label)`, chevron fixo
    - Se `item.isDestructive == true` → label em cor `error_red`
    - `contentDescription` no ícone: `item.label`
    - `contentDescription` no chevron: `"Abrir ${item.label}"`
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none (adapter simples)
- **Gate**: quick
- **Commit**: `feat(adapter): add SettingsAdapter for profile settings list`

---

### T9: Criar PlanAdapter (RecyclerView) [P]
- **What**: Criar adapter RecyclerView para a lista de cards de planos com suporte a expansão e destaque.
- **Where**: `app/src/main/java/com/example/esttufa/adapter/PlanAdapter.kt`
- **Depends on**: T1, T5, T7
- **Reuses**: Nenhum RecyclerView.Adapter existente (novo padrão no app)
- **Requirement ID**: `PLAN-02`, `PLAN-03`, `PLAN-04`, `PLAN-05`, `PLAN-08`, `ACC-01`, `ACC-03`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] `PlanAdapter.kt` criado como `RecyclerView.Adapter<PlanAdapter.PlanViewHolder>` com:
    - `PlanViewHolder` com View Binding de `item_plan_card.xml`
    - Propriedade `currentPlanId: String` (atualizado via setter)
    - Propriedade `expandedPlanIds: MutableSet<String>` para rastrear expansão
    - Interface `OnPlanActionListener` com: `onSubscribeClick(plan: Plan)`, `onSeeMoreClick(planId: String)`
    - Bind:
      - Nome: `plan.emoji + " " + plan.name`
      - Preço: `plan.priceRange`
      - Benefícios: se expandido → mostra todos; senão → mostra `plan.maxVisibleBenefits` + "ver mais"
      - Badge "Mais popular": `VISIBLE` se `plan.isRecommended`
      - Card border: se `plan.isRecommended` → `strokeColor = tertiary`, `strokeWidth = 2dp`
      - Botão: se `plan.id == currentPlanId` → "Plano atual" disabled; senão → "Assinar" enabled, cor `tertiary`
    - `contentDescription` no badge: `"Plano recomendado: ${plan.name}"`
  - [ ] Dependência RecyclerView verificada em `build.gradle.kts` (já incluída via `material` library)
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(adapter): add PlanAdapter RecyclerView adapter with expansion and highlight`

---

### T10: Criar ProfileActivity
- **What**: Criar a Activity da tela de Perfil com View Binding, observação do ProfileViewModel e navegação.
- **Where**: `app/src/main/java/com/example/esttufa/ProfileActivity.kt`
- **Depends on**: T3, T6, T8
- **Reuses**: `HomeActivity.kt` (padrão de Activity com View Binding + ViewModel)
- **Requirement ID**: `PERF-01` a `PERF-10`
- **Done when**:
  - [ ] `ProfileActivity.kt` criado com:
    - View Binding: `ActivityProfileBinding`
    - ViewModel: `ProfileViewModel` via `by viewModels()`
    - `onCreate`:
      - Inflate binding
      - Setup toolbar com botão de voltar (`setSupportActionBar` + `setDisplayHomeAsUpEnabled`)
      - Setup avatar: se foto disponível → carregar; senão → iniciais sobre fundo `primary`
      - Setup `SettingsAdapter` no ListView/LinearLayout de configurações
      - Observar `uiState` para alternar entre Loading/Success/Error
    - `observeViewModel()`:
      - `Loading` → mostrar ProgressBar, ocultar conteúdo
      - `Success` → preencher campos, ocultar ProgressBar
      - `Error` → mostrar error state com botão retry
    - Click listeners:
      - "Ver todos os planos" → `startActivity(Intent(this, PlansActivity::class.java))`
      - Item "Sair" → `showLogoutDialog()`
      - Botões de edição nos campos → Toast "Em breve" (placeholder para iteração futura)
    - `showLogoutDialog()`:
      - `MaterialAlertDialogBuilder` com título, mensagem, botão positivo (Sair → signOut + navigate to MainActivity), botão negativo (Cancelar)
    - `onSupportNavigateUp()` → `finish()` e retorna `true`
    - Avatar com iniciais: extrair primeira letra do nome e sobrenome, exibir em `TextView` centralizado sobre `bg_avatar_initials`
    - `configChanges="orientation|screenSize|screenLayout"` no Manifest
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none (smoke test na T13)
- **Gate**: quick
- **Commit**: `feat(profile): add ProfileActivity with user data and settings`

---

### T11: Criar PlansActivity e ConfirmSubscriptionBottomSheet
- **What**: Criar a Activity da tela de Planos e o BottomSheetDialogFragment de confirmação.
- **Where**:
  - `app/src/main/java/com/example/esttufa/PlansActivity.kt`
  - `app/src/main/java/com/example/esttufa/ConfirmSubscriptionBottomSheet.kt`
- **Depends on**: T4, T7, T9
- **Reuses**: `HomeActivity.kt` (padrão de Activity)
- **Requirement ID**: `PLAN-01` a `PLAN-11`
- **Done when**:
  - [ ] `PlansActivity.kt` criado com:
    - View Binding: `ActivityPlansBinding`
    - ViewModel: `PlansViewModel` via `by viewModels()`
    - `onCreate`:
      - Inflate binding
      - Setup toolbar com botão de voltar
      - Setup RecyclerView com `LinearLayoutManager` e `PlanAdapter`
      - Observar `uiState` e `subscriptionEvent`
    - `observeViewModel()`:
      - `Loading` → ProgressBar visível, RecyclerView oculto
      - `Success` → popular `PlanAdapter` com planos e `currentPlanId`
      - `Error` → mostrar error state
    - `observeEvents()`:
      - `ConfirmSubscription(plan)` → abrir `ConfirmSubscriptionBottomSheet.newInstance(plan)`
      - `SubscriptionSuccess(name)` → Snackbar com mensagem de sucesso, recarregar planos
      - `SubscriptionError(msg)` → Snackbar de erro
    - `PlanAdapter.OnPlanActionListener`:
      - `onSubscribeClick` → `viewModel.requestSubscription(plan)`
      - `onSeeMoreClick` → `viewModel.toggleCardExpansion(planId)`, notificar adapter
    - `onSupportNavigateUp()` → `finish()`
  - [ ] `ConfirmSubscriptionBottomSheet.kt` criado como `BottomSheetDialogFragment` com:
    - View Binding: `BottomSheetConfirmSubscriptionBinding`
    - Companion `fun newInstance(plan: Plan): ConfirmSubscriptionBottomSheet` — serializa dados via arguments Bundle
    - `onCreateView` → inflate layout
    - `onViewCreated` → preencher nome, preço, top 3 benefícios com ícone de check
    - Botão "Confirmar" → callback para Activity (via interface ou parentFragmentManager/result)
    - Botão "Cancelar" → `dismiss()`
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none (smoke test na T13)
- **Gate**: quick
- **Commit**: `feat(plans): add PlansActivity and ConfirmSubscriptionBottomSheet`

---

### T12: Integrar com HomeActivity e AndroidManifest
- **What**: Modificar `HomeActivity` para navegar ao Perfil ao tocar no avatar/cabeçalho. Registrar novas Activities no Manifest.
- **Where**:
  - `app/src/main/java/com/example/esttufa/HomeActivity.kt`
  - `app/src/main/res/layout/activity_home.xml`
  - `app/src/main/AndroidManifest.xml`
- **Depends on**: T10, T11
- **Reuses**: Padrão de navegação via `Intent` existente
- **Requirement ID**: `PERF-01`
- **Done when**:
  - [ ] `HomeActivity.kt` modificado:
    - Adicionado `binding.ivAvatar.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }` no `setupUI()`
    - Alternativa: tornar todo o `llHeader` clicável para melhor UX
  - [ ] `activity_home.xml` modificado:
    - `android:clickable="true"` e `android:focusable="true"` no `ivAvatar` ou `llHeader`
    - `contentDescription` adequado ("Abrir perfil")
  - [ ] `AndroidManifest.xml` modificado — adicionar:
    ```xml
    <activity
        android:name=".ProfileActivity"
        android:configChanges="orientation|screenSize|screenLayout"
        android:exported="false" />
    <activity
        android:name=".PlansActivity"
        android:configChanges="orientation|screenSize|screenLayout"
        android:exported="false" />
    ```
  - [ ] Build compila sem erros: `./gradlew assembleDebug`
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(navigation): connect Home to Profile and register new activities`

---

### T13: Verificação Final — Build e Smoke Test
- **What**: Garantir que o projeto compila sem erros e que o fluxo Home → Perfil → Planos funciona corretamente.
- **Where**: Projeto inteiro
- **Depends on**: T12
- **Reuses**: N/A
- **Requirement ID**: Todos
- **Done when**:
  - [ ] `./gradlew assembleDebug` completa com BUILD SUCCESSFUL
  - [ ] `./gradlew lint` não introduz novos warnings críticos
  - [ ] Fluxo manual verificável:
    1. Home → toque no avatar → abre Perfil
    2. Perfil mostra dados do Firebase (nome, email)
    3. Perfil → "Ver todos os planos" → abre Planos
    4. Planos mostra 5 cards com dados corretos
    5. Card "Muda" tem badge "Mais popular" e botão "Plano atual" desabilitado
    6. Outros cards têm botão "Assinar" ativo
    7. Clicar "Assinar" → bottom sheet com resumo → "Confirmar" → Snackbar sucesso
    8. "ver mais" expande benefícios do card
    9. Voltar dos Planos → Perfil → Home (back stack correta)
    10. "Sair" no Perfil → diálogo → confirmar → volta para Login (MainActivity)
  - [ ] Acessibilidade: TalkBack lê corretamente os elementos essenciais
- **Tests**: build + lint
- **Gate**: full
- **Commit**: `chore: verify build and smoke test for Profile and Plans screens`

---

## 6. Resumo de Acessibilidade

| Elemento | `contentDescription` |
|---|---|
| Avatar na Home | "Abrir perfil" |
| Avatar no Perfil | "Foto do perfil" ou "Iniciais do nome: JS" |
| Badge de plano | "Plano atual: Muda" |
| Botão de edição em cada campo | "Editar campo" |
| Ícone de notificações | "Notificações" |
| Ícone de idioma | "Idioma" |
| Ícone de suporte | "Suporte" |
| Ícone de termos | "Termos de uso" |
| Ícone de sair | "Sair" |
| Chevron em itens de config | "Abrir Notificações" (dinâmico) |
| Badge "Mais popular" | "Plano recomendado: Muda" |
| Botão "Assinar" | "Assinar Plano Colheita" (dinâmico) |
| Botão "Plano atual" | "Plano atual — já assinado" |
| Botão "ver mais" | "Ver mais benefícios do Plano Safra" (dinâmico) |
| Botão voltar (toolbar) | "Voltar" (padrão Android) |

Todos os tamanhos de texto usam `sp` para respeitar a configuração de escala de fonte do sistema. Elementos interativos possuem `minHeight` de 48dp conforme recomendações de acessibilidade do Material Design.
