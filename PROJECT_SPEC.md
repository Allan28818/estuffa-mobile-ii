# Project Spec Map

Last updated: 2026-06-13

## Purpose

Aplicativo Android para autenticação, cadastro de estufas e consulta de dados de
irrigação associados às culturas do usuário.

## Architecture Summary

- Pattern: MVVM leve.
- Main runtime: Android/Kotlin com Activities, View Binding e LiveData.
- Main data flow: Activity -> ViewModel -> Repository -> Retrofit/OkHttp -> API.
- Persistence: sessão local gerenciada pelo Firebase Authentication.
- External integrations: Firebase Auth e API REST Esttufa.
- Validation strategy: testes unitários focados, build Gradle e smoke tests.

## Module Index

| Module | Responsibility | Key files | Notes |
| --- | --- | --- | --- |
| `presentation/auth` | Login, cadastro e navegação de sessão. | `MainActivity.kt`, `CadastroActivity.kt` | Deve manter detalhes Firebase fora da renderização. |
| `presentation/stoves` | Listagem, criação e detalhes de estufas. | `HomeActivity.kt`, `CadastroEstufaActivity.kt`, `CulturaInfoActivity.kt` | Nomes legados de UI permanecem por compatibilidade. |
| `viewmodel` | Estado e orquestração assíncrona das telas. | `viewmodel/*.kt` | Usa LiveData e `viewModelScope`. |
| `repository` | Fronteira das operações remotas. | `repository/*.kt` | Retorna `Result<T>`. |
| `network/model` | Retrofit, autenticação HTTP e DTOs. | `model/*.kt` | Não deve conter estado de tela. |
| `resources` | Layouts, strings e imagens locais. | `res/layout`, `res/drawable`, `res/values` | View Binding habilitado. |

## Module Specs

### Module: `presentation/auth`

Responsibility:

- Validar entrada visual, observar estado de autenticação e navegar.
- Não executar chamadas Retrofit nem mapear DTOs.

Important files:

- `MainActivity.kt`: entrada do app, login e auto-login.
- `CadastroActivity.kt`: criação de conta e perfil.

Public interfaces:

- Intents para `CadastroActivity` e `HomeActivity`.
- Estados publicados pelos ViewModels de autenticação.

Dependencies:

- Internal dependencies: ViewModels e View Binding.
- External dependencies: Firebase Authentication.

Data flow:

- Inputs: email, senha, nome e sobrenome.
- Processing: validação local e operação Firebase.
- Outputs: estado de sucesso/erro e navegação.

Validation:

- Tests: validações e transições de estado quando desacopladas.
- Manual checks: login, cadastro, erro e auto-login.
- Known gaps: Firebase real exige credenciais e dispositivo/emulador.

Refactoring notes:

- Hotspots: mensagens de erro do Firebase e controle de loading.
- Risks: navegar antes de concluir `updateProfile`.
- Safe next steps: centralizar tradução de erros de autenticação.

### Module: `presentation/stoves`

Responsibility:

- Exibir, criar e abrir detalhes de estufas do usuário autenticado.
- Não conhecer o formato JSON da API.

Important files:

- `HomeActivity.kt`: lista, empty state, saudação e navegação.
- `CadastroEstufaActivity.kt`: formulário de criação.
- `CulturaAdapter.kt`: nome da estufa e imagem local por cultura.
- `CulturaInfoActivity.kt`: sensores, câmera e consulta de irrigação.

Public interfaces:

- Extras `stove_id`, `crop` e `stove_name` ao abrir detalhes.
- `RESULT_OK` ao concluir a criação.

Dependencies:

- Internal dependencies: ViewModels, adapter e recursos.
- External dependencies: Android Activity Result API.

Data flow:

- Inputs: ações do usuário e estados dos ViewModels.
- Processing: renderização e navegação.
- Outputs: lista, empty state, feedback e intents.

Validation:

- Tests: mapeamento de cultura para drawable.
- Manual checks: criação, recarga e abertura de detalhes.
- Known gaps: sensores e câmera exigem dispositivo.

Refactoring notes:

- Hotspots: `CulturaInfoActivity` concentra muitas responsabilidades.
- Risks: chamadas duplicadas da Home em `onCreate`/`onResume`.
- Safe next steps: extrair componentes de sensores e captura de imagem.

### Module: `viewmodel`

Responsibility:

- Orquestrar autenticação e operações de estufas.
- Publicar estados imutáveis para a UI.

Important files:

- `HomeViewModel.kt`: listagem e estado vazio/loading.
- `CadastroEstufaViewModel.kt`: criação e mapeamento PT -> EN.
- `LoginViewModel.kt`: login Firebase.
- `CadastroViewModel.kt`: registro e atualização de perfil.
- `CulturaInfoViewModel.kt`: consulta de irrigação.

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

- `StoveRepository.kt`: CRUD completo de estufas.
- `IrrigationRepository.kt`: consulta de irrigação.

Public interfaces:

- Operações suspensas de estufas e irrigação.

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

- `app/build.gradle.kts`: dependências Android, Retrofit e Firebase Auth.
- `RetrofitClient.kt`: configuração única de Retrofit/OkHttp.
- `AuthInterceptor.kt`: token Firebase e resposta não autorizada.
- `ApiService.kt`: contrato REST.
- `StoveRequest.kt`: bodies de criação e edição.
- `StoveResponse.kt`: representação remota de uma estufa.
- `StoveListResponse.kt`: envelope da listagem.

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
- Manual checks: header Authorization nos logs de debug.
- Known gaps: integração depende de sessão Firebase válida.

Refactoring notes:

- Hotspots: singleton e redirecionamento global em `401`.
- Risks: logging BODY expõe token em builds não debug.
- Safe next steps: condicionar logging a `BuildConfig.DEBUG`.

## Cross-Module Relationships

- `presentation` -> `viewmodel`: ações e observação de estado.
- `viewmodel` -> `repository`: execução de operações remotas.
- `repository` -> `network/model`: contrato Retrofit e DTOs.
- `network/model` -> Firebase: aquisição de token e encerramento de sessão.

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
