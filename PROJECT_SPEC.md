# Project Spec Map

Last updated: 2026-06-16

## Purpose

Aplicativo Android para autenticação, cadastro de estufas e consulta de dados de
irrigação associados às culturas do usuário.

## Architecture Summary

- Pattern: MVVM leve.
- Main runtime: Android/Kotlin com Activities, View Binding e LiveData.
- Main data flow: Activity -> ViewModel -> Repository -> Retrofit/OkHttp -> API.
- Persistence: sessão local gerenciada pelo Firebase Authentication; Room e
  SharedPreferences entram na spec 5 para leituras e preferências.
- External integrations: Firebase Auth, API REST Esttufa e Maps SDK.
- Validation strategy: testes unitários focados, build Gradle e smoke tests.

## Module Index

| Module | Responsibility | Key files | Notes |
| --- | --- | --- | --- |
| `presentation/auth` | Login, cadastro, recuperação de senha e navegação. | `MainActivity.kt`, `CadastroActivity.kt`, `EsqueciSenhaActivity.kt` | Deve manter detalhes Firebase fora da renderização. |
| `presentation/stoves` | Listagem, criação e detalhes de estufas. | `HomeActivity.kt`, `CadastroEstufaActivity.kt`, `CulturaInfoActivity.kt` | Nomes legados de UI permanecem por compatibilidade. |
| `presentation/profile-plans` | Perfil, configurações e catálogo de assinatura. | `SettingsAdapter.kt`, layouts de perfil/planos | Fluxo local nesta iteração. |
| `viewmodel` | Estado e orquestração assíncrona das telas. | `viewmodel/*.kt` | Usa LiveData e `viewModelScope`. |
| `repository` | Fronteira das operações remotas. | `repository/*.kt` | Retorna `Result<T>`. |
| `network/model` | Retrofit, autenticação HTTP e DTOs. | `model/*.kt` | Não deve conter estado de tela. |
| `local` | Persistência Room e SharedPreferences. | `local/*.kt` | Introduzido pela spec 5. |
| `profile/plans model` | Contratos locais de perfil, catálogo e configurações. | `UserProfile.kt`, `Plan.kt`, `SettingsItem.kt` | Não depende de Android UI. |
| `warming` | Pré-aquecimento idempotente da API. | `warming/ApiWarmingHelper.kt` | Falhas são silenciosas e não bloqueiam a UI. |
| `resources` | Layouts, strings e imagens locais. | `res/layout`, `res/drawable`, `res/values` | View Binding habilitado. |

## Module Specs

### Module: `local`

Responsibility:

- Persistir dados locais que não pertencem à sessão Firebase ou à API remota.

Important files:

- `local/SensorReadingEntity.kt`: entidade Room de leituras de sensores com
  cultura, versão, temperatura, umidade, luminosidade, irrigação e timestamp.
- `local/SensorReadingDao.kt`: operações Room de inserção e consulta das
  leituras salvas.
- `local/RoomAppDatabase.kt`: singleton da database local `esttufa-local.db`.

Public interfaces:

- `RoomAppDatabase.getInstance(context)`.
- `SensorReadingDao.insert`, `getAll` e `getByStoveId`.

Dependencies:

- Android `Context` e Room runtime/KSP.

Data flow:

- Repositories locais recebem dados da camada de aplicação e gravam via DAO.

Validation:

- `gradlew.bat assembleDebug --console=plain`: BUILD SUCCESSFUL em 2026-06-17.

Refactoring notes:

- Quando houver migrações, ativar schema export ou adicionar estratégia de
  migração explícita antes de aumentar a versão da database.

### Module: `resources`

Responsibility:

- Definir textos, cores, vetores e fundos reutilizados pelo perfil e planos.

Important files:

- `res/values/strings.xml`: textos visíveis e descrições de acessibilidade.
- `res/values/colors.xml`: cores de badge, erro, divisão e estado desabilitado.
- `res/drawable/ic_*.xml`: ícones de configuração e edição.
- `res/drawable/bg_*.xml`: avatar, badge e destaque de plano recomendado.
- `res/layout/activity_profile.xml`: conteúdo e estados da tela de perfil.
- `res/layout/item_setting.xml`: linha acessível de configuração.
- `res/layout/activity_plans.xml`: lista e estados da tela de planos.
- `res/layout/item_plan_card.xml`: card de plano expansível.
- `res/layout/bottom_sheet_confirm_subscription.xml`: resumo da assinatura.

Validation:

- Merge de recursos e build Android.

### Module: `presentation/profile-plans`

Responsibility:

- Exibir identidade, assinatura, configurações e catálogo de planos.
- Navegar entre Home, Perfil e Planos sem conhecer contratos HTTP.

Important files:

- `ProfileActivity.kt`: renderiza estado, avatar, configurações e logout.
- `PlansActivity.kt`: renderiza catálogo/estados e coordena assinatura.
- `ConfirmSubscriptionBottomSheet.kt`: resume e confirma o plano selecionado.
- `SettingsAdapter.kt`: vincula os atalhos da seção de configurações.
- `PlanAdapter.kt`: aplica regras visuais e ações dos cards de plano.

Dependencies:

- ViewModels, View Binding, Firebase Auth indireto e recursos Android.

Data flow:

- ViewModel publica perfil/catálogo; Activities renderizam e encaminham ações.

Validation:

- Build, lint e smoke do fluxo de navegação.
- Em 2026-06-14, testes unitários, lint e assemble concluíram com sucesso.
- Smoke em dispositivo permanece pendente porque `adb devices` não respondeu
  no ambiente de execução.

### Module: `presentation/auth`

Responsibility:

- Validar entrada visual, observar estado de autenticação e navegar.
- Não executar chamadas Retrofit nem mapear DTOs.

Important files:

- `MainActivity.kt`: valida credenciais, observa o login e executa auto-login.
- `CadastroActivity.kt`: valida o formulário e observa a criação da conta.
- `EsqueciSenhaActivity.kt`: valida o e-mail, observa o envio da redefinição e
  retorna ao login após sucesso.
- `auth/UnauthorizedSessionHandler.kt`: encerra a sessão e limpa a pilha de
  Activities após uma resposta HTTP `401`.

Public interfaces:

- Intents para `CadastroActivity`, `EsqueciSenhaActivity` e `HomeActivity`.
- Estados publicados pelos ViewModels de autenticação.

Dependencies:

- Internal dependencies: ViewModels e View Binding.
- External dependencies: Firebase Authentication.

Data flow:

- Inputs: email, senha, nome e sobrenome.
- Processing: validação local e operação Firebase.
- Outputs: estado de sucesso/erro, Toasts e navegação.

Validation:

- Tests: validações e transições de estado quando desacopladas.
- Manual checks: login, cadastro, erro e auto-login.
- Known gaps: envio e template de recuperação exigem Firebase real e
  dispositivo/emulador.

Refactoring notes:

- Hotspots: mensagens de erro do Firebase e controle de loading.
- Risks: navegar antes de concluir `updateProfile`.
- Safe next steps: centralizar tradução de erros de autenticação.

### Module: `presentation/stoves`

Responsibility:

- Exibir, criar e abrir detalhes de estufas do usuário autenticado.
- Não conhecer o formato JSON da API.

Important files:

- `HomeActivity.kt`: observa `StoveResponse`, recarrega após retornos, usa o
  nome Firebase na saudação, abre detalhes e oferece entrada para o perfil.
- `CadastroEstufaActivity.kt`: valida o formulário, observa a criação e retorna
  `RESULT_OK` para recarregar a Home.
- `CulturaAdapter.kt`: renderiza nome da estufa e drawable local por cultura,
  sem URL remota.
- `CulturaInfoActivity.kt`: sensores, câmera, classificação de imagem e
  consulta de irrigação; captura fotos completas em cache por `FileProvider`.
- `AndroidManifest.xml`: permissões do app; câmera declarada como hardware
  opcional e provider privado para compartilhar a saída com o app de câmera.
- `res/xml/file_paths.xml`: limita o compartilhamento ao cache
  `captured_images`.
- `activity_cultura_info.xml`: mantém o preview separado do estado de
  sucesso/erro para a foto não desaparecer após a classificação.

Public interfaces:

- Extras `stove_id`, `crop`, `cultura` e `stove_name` ao abrir detalhes.
- `RESULT_OK` ao concluir a criação.
- Intent explícito da Home para `ProfileActivity`.

Dependencies:

- Internal dependencies: ViewModels, adapter e recursos.
- External dependencies: Android Activity Result API.

Data flow:

- Inputs: ações do usuário e estados dos ViewModels.
- Processing: renderização, navegação, captura em resolução real e conversão
  de imagens para multipart.
- Outputs: lista, empty state, feedback e intents.

Validation:

- Tests: build, lint e smoke em emulador.
- Manual checks: criação, recarga, imagem local e abertura de detalhes.
- Em 2026-06-15, o smoke no emulador confirmou captura, preview, upload e
  classificação real com resposta HTTP 200.

Refactoring notes:

- Hotspots: `CulturaInfoActivity` concentra muitas responsabilidades.
- Risks: chamadas duplicadas da Home em `onCreate`/`onResume`.
- Safe next steps: extrair componentes de sensores e captura de imagem.

### Module: `viewmodel`

Responsibility:

- Orquestrar autenticação e operações de estufas.
- Publicar estados imutáveis para a UI.

Important files:

- `HomeViewModel.kt`: lista estufas e publica estados vazio/loading.
- `CadastroEstufaViewModel.kt`: criação, mapeamento PT -> EN e mensagem
  específica para rejeições HTTP `422`.
- `LoginViewModel.kt`: login Firebase e tradução de falhas comuns.
- `CadastroViewModel.kt`: cria a conta e só conclui após atualizar o
  `displayName`.
- `EsqueciSenhaViewModel.kt`: solicita redefinição via Firebase Auth e traduz
  falhas comuns em mensagens legíveis.
- `CulturaInfoViewModel.kt`: consulta de irrigação e estado independente de
  classificação de plantas.
- `ProfileViewModel.kt`: carrega identidade/assinatura e expõe configurações.
- `PlansViewModel.kt`: publica catálogo, plano atual, expansão e eventos de
  confirmação de assinatura.

Public interfaces:

- Métodos de ação e `LiveData` de estado.

Dependencies:

- Internal dependencies: repositories.
- External dependencies: Lifecycle KTX e Firebase Auth.

Data flow:

- Inputs: comandos da UI.
- Processing: coroutines/tasks e mapeamento de erros.
- Outputs: estados Idle/Loading/Success/Error.

Validation:

- Tests: regras de validação e mapeamento de cultura.
- Manual checks: observação dos estados nas Activities.
- Known gaps: Tasks do Firebase demandam doubles para testes isolados.

Refactoring notes:

- Hotspots: Firebase Tasks baseadas em callbacks.
- Risks: eventos de navegação serem reemitidos após rotação.
- Safe next steps: migrar eventos únicos para wrapper/event flow.

### Module: `repository`

Responsibility:

- Encapsular chamadas Retrofit e retornar `Result`.
- Não decidir navegação ou renderização.

Important files:

- `StoveRepository.kt`: CRUD completo com `ApiService` injetável e resultados
  explícitos para a camada de aplicação.
- `IrrigationRepository.kt`: consulta de irrigação.
- `PlantClassificationRepository.kt`: envia multipart ao classificador e
  preserva falhas em `Result`.
- `UserRepository.kt`: monta o perfil a partir do Firebase Auth e mocks locais.
- `PlanRepository.kt`: fornece o catálogo e mantém o plano atual no processo.

Public interfaces:

- Operações de estufas, irrigação, classificação, perfil e catálogo de planos.

Dependencies:

- Internal dependencies: `RetrofitClient` e DTOs.
- External dependencies: Retrofit.

Data flow:

- Inputs: parâmetros dos casos de uso.
- Processing: chamada HTTP dentro de `try/catch`.
- Outputs: `Result<T>`.

Validation:

- Tests: contratos com `ApiService` injetável quando necessário.
- Manual checks: respostas da API de produção.
- Known gaps: cliente singleton dificulta doubles.

Refactoring notes:

- Hotspots: dependência direta do singleton Retrofit.
- Risks: mensagens de erro pouco legíveis sem mapear `HttpException`.
- Safe next steps: injetar `ApiService` nos repositories.

### Module: `network/model`

Responsibility:

- Definir DTOs, endpoints e política de autenticação HTTP.
- Não conter regras de apresentação.

Important files:

- `app/build.gradle.kts`: dependências Android, Retrofit, Firebase Auth, KSP,
  Room e Maps SDK.
- `gradle.properties`: mantém `android.disallowKotlinSourceSets=false` para
  permitir KSP com o Kotlin embutido do AGP 9 enquanto o plugin ainda adiciona
  fontes geradas pela DSL de Kotlin.
- `RetrofitClient.kt`: configura Retrofit/OkHttp com autenticação antes do
  logging para compartilhar o header entre todos os endpoints.
- `AuthInterceptor.kt`: força a atualização do ID token, preserva requisições
  públicas sem sessão e aciona o encerramento global em respostas `401`.
- `ApiService.kt`: contrato REST de irrigação, estufas, classificação
  multipart e healthcheck.
- `PlantClassificationResponse.kt`: resposta resiliente com nomes de classe e
  confiança opcionais; prioriza o campo real `prediction` e mantém os aliases
  legados `predicted_class` e `class_name`.
- `StoveRequest.kt`: bodies de criação e edição.
- `StoveResponse.kt`: mapeia identidade, nome, cultura, proprietário e
  timestamps retornados pela API.
- `StoveListResponse.kt`: mapeia o envelope `{ "stoves": [...] }`.

Public interfaces:

- `RetrofitClient.api`.
- Métodos Retrofit de estufas e irrigação.

Dependencies:

- Internal dependencies: DTOs.
- External dependencies: OkHttp, Retrofit, Gson e Firebase Auth.

Data flow:

- Inputs: requests dos repositories.
- Processing: token, serialização e chamada HTTP.
- Outputs: DTOs ou exceções Retrofit.

Validation:

- Tests: interceptor com doubles quando o desenho permitir.
- Manual checks: autenticação e CRUD validados contra produção.
- `PlantClassificationResponseTest.kt`: valida desserialização de `prediction`
  e compatibilidade com os nomes legados.
- Known gaps: não há teste unitário isolado do interceptor.

Refactoring notes:

- Hotspots: singleton e redirecionamento global em `401`.
- Risks: logging BODY expõe token em builds não debug.
- Safe next steps: condicionar logging a `BuildConfig.DEBUG`.

Legacy removed:

- O DTO `Cultura`, seu envelope, a rota `/stoves/list` e o carregamento remoto
  por Glide não fazem mais parte do app.

### Module: `profile/plans model`

Responsibility:

- Representar identidade exibida, assinatura local, catálogo de planos e itens
  de configuração sem acoplamento com Activities ou Firebase.

Important files:

- `UserProfile.kt`: dados do usuário e assinatura exibidos no perfil.
- `Plan.kt`: preço, benefícios, recomendação e apresentação de cada plano.
- `SettingsItem.kt`: metadados dos atalhos de configuração.

Public interfaces:

- Data classes imutáveis consumidas por repositories, ViewModels e adapters.

Dependencies:

- Nenhuma dependência de framework.

Validation:

- Compilação Kotlin no build Android.

Refactoring notes:

- O plano atual permanece local até existir contrato de assinatura na API.

## Cross-Module Relationships

- `presentation` -> `viewmodel`: ações e observação de estado.
- `viewmodel` -> `repository`: execução de operações remotas.
- `repository` -> `network/model`: contrato Retrofit e DTOs.
- `network/model` -> Firebase: aquisição de token e encerramento de sessão.
- `presentation/auth` e `presentation/stoves` -> `warming`: disparam o
  healthcheck idempotente no startup.

## Refactoring Map

- Hotspot: `CulturaInfoActivity` monolítica.
- Why it matters: mistura sensores, câmera, timer e renderização de rede.
- Suggested next step: extrair controladores após estabilizar o CRUD.
- Risk level: medium.

- Hotspot: `RetrofitClient` singleton.
- Why it matters: reduz testabilidade de repositories e interceptores.
- Suggested next step: introduzir fábrica/injeção manual em história futura.
- Risk level: low.

## Open Questions

- O smoke test completo depende da disponibilidade da API e de uma conta
  Firebase válida no ambiente de execução.
- A classificação por câmera/galeria ainda requer smoke em dispositivo porque
  o ambiente atual não possui `adb` e não alcançou a API externa.
- O template de recuperação deve ser aplicado no Console Firebase do projeto
  `esttufa-ai`; o repositório não possui infraestrutura ou credenciais para
  alterar essa configuração remota.
