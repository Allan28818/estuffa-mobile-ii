# Planning Spec: Classificação de Imagem via API + Warming da API no Startup
**Status**: DONE
**Started at**: 2026-06-14 15:28
**Completed at**: 2026-06-14
**Priority**: extra-high

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
- **Build**: View Binding habilitado. Sem Compose, sem Hilt/Dagger.
- **API de produção**: `https://api-esttufa.onrender.com/` (Render — free tier, dorme após inatividade)
- **Observed Conventions & Patterns**:
  - MVVM: Activity → ViewModel → Repository → RetrofitClient (singleton) → ApiService (Retrofit)
  - ViewModels usam LiveData + viewModelScope com coroutines
  - Repositories retornam `Result<T>` (Kotlin Result wrapper)
  - Activities usam View Binding
- **Risks & Fragile Areas**:
  - `CulturaInfoActivity.processImageResult()` usa `Random().nextInt(10) < 7` para simular reconhecimento — a imagem NUNCA é enviada para a API
  - Não existe nenhum endpoint de classificação de planta definido no `ApiService.kt`
  - Não existe nenhum DTO para upload de imagem ou resposta de classificação
  - A API no Render (free tier) dorme após inatividade e pode levar 30-60s para acordar na primeira requisição
  - Nenhum mecanismo de warming/pre-aquecimento existe no app

---

## 2. Requirements & Acceptance Criteria
- **Goal**: Corrigir a funcionalidade de classificação de imagem na tela de detalhes da estufa (`CulturaInfoActivity`) para enviar a imagem capturada/selecionada ao endpoint real `POST /plant-classification/predict` da API, exibindo o resultado retornado pela API. Adicionalmente, implementar um mecanismo de warming que faz uma requisição ao endpoint de healthcheck (`GET /hearth-beat`) assim que o usuário entra na primeira tela do app, para pré-aquecer a API do Render.

- **Acceptance Criteria**:

  ### Bug Fix — Classificação de Imagem via API
  - `IMG-01`: WHEN o usuário captura uma foto via câmera OU seleciona uma imagem da galeria na `CulturaInfoActivity` THEN o app SHALL enviar a imagem ao endpoint `POST /plant-classification/predict` como `multipart/form-data` com o campo `image`, e o query param `model` com valor `decision_tree`.
  - `IMG-02`: WHEN a API retorna sucesso (200) na classificação THEN o app SHALL exibir a imagem capturada/selecionada no `ivFotoResult`, exibir o nome da cultura classificada no `tvNomeCulturaResult` com o valor retornado pela API, e tornar visível o card `cvFotoReconhecida`.
  - `IMG-03`: WHEN a API retorna um erro (4xx, 5xx, timeout, sem rede) na classificação THEN o app SHALL exibir o card `cvFotoNaoReconhecida` e ocultar o card `cvFotoReconhecida`.
  - `IMG-04`: WHEN a classificação está em andamento THEN o app SHALL exibir um indicador de loading (pode usar o `progressBar` existente ou similar) e impedir que o usuário envie uma nova imagem enquanto a anterior está sendo processada.
  - `IMG-05`: WHEN o usuário abre a tela de detalhes THEN o texto de instrução (`tvInstrucaoFoto`) SHALL estar visível até que o usuário capture/selecione uma imagem pela primeira vez.

  ### Feature — Warming da API
  - `WARM-01`: WHEN o app é aberto e a `MainActivity` (tela de login) é carregada em `onCreate()` THEN o app SHALL disparar uma requisição `GET /hearth-beat` em background (fire-and-forget, sem bloquear a UI) para acordar a API do Render.
  - `WARM-02`: WHEN a requisição de warming falha (timeout, sem rede, erro HTTP) THEN o app SHALL ignorar silenciosamente o erro — NÃO exibir Toast, NÃO crashar, NÃO impactar o fluxo do usuário.
  - `WARM-03`: WHEN o usuário já está autenticado e o app faz auto-login direto para `HomeActivity` THEN o warming SHALL também ser disparado (no `HomeActivity.onCreate()` como fallback, caso o `MainActivity` tenha sido pulado rapidamente).

---

## 3. Design & Impact Analysis

### Fluxo de Classificação de Imagem (Corrigido)
```
CulturaInfoActivity
  │ usuário tira foto / escolhe da galeria
  ▼
processImageResult(bitmap?, uri?)
  │ converte para arquivo/byte array
  ▼
CulturaInfoViewModel.classifyImage(imagePart)
  │
  ▼
PlantClassificationRepository.predict(imagePart, model)
  │
  ▼
ApiService.predictPlantClassification(model, imagePart)
  │ POST /plant-classification/predict
  │ Content-Type: multipart/form-data
  │ Field: image (binary)
  │ Query: model=decision_tree
  ▼
Response: { predicted_class: "...", confidence: ..., ... }
  │
  ▼
CulturaInfoActivity
  ├─ Sucesso → exibe cvFotoReconhecida + nome retornado
  └─ Erro → exibe cvFotoNaoReconhecida
```

### Fluxo de Warming
```
App Launch
  │
  ▼
MainActivity.onCreate()
  │ fire-and-forget coroutine
  │ GET /hearth-beat
  │ (ignora resultado)
  ▼
API Render acorda do sleep
  │ (pronta para requisições subsequentes)
```

### API Endpoints Descobertos (OpenAPI spec)

| Endpoint | Method | Uso |
|---|---|---|
| `GET /` | GET/HEAD | Root Health Check |
| `GET /hearth-beat` | GET | Heart Beat (warming) |
| `POST /plant-classification/predict` | POST | Classificação de planta (multipart: image) |
| `GET /irrigation-time/sensor-simulated/{crop_name}` | GET | Tempo de irrigação simulado (já usado) |

### Code Reuse Blueprints
- **Repository pattern**: `repository/IrrigationRepository.kt` — blueprint com `Result<T>`
- **ViewModel pattern**: `viewmodel/CulturaInfoViewModel.kt` — sealed class UiState
- **Multipart upload**: OkHttp `MultipartBody.Part` + Retrofit `@Multipart` + `@Part`

### Files to Modify
- `app/src/main/java/com/example/esttufa/model/ApiService.kt`: Adicionar endpoint `POST /plant-classification/predict` com `@Multipart` e endpoint `GET /hearth-beat`
- `app/src/main/java/com/example/esttufa/viewmodel/CulturaInfoViewModel.kt`: Adicionar função `classifyImage()` + novo estado de UI para classificação
- `app/src/main/java/com/example/esttufa/CulturaInfoActivity.kt`: Refatorar `processImageResult()` para chamar a API ao invés de usar `Random()`, observar novo estado do ViewModel
- `app/src/main/java/com/example/esttufa/MainActivity.kt`: Adicionar chamada de warming em `onCreate()`
- `app/src/main/java/com/example/esttufa/HomeActivity.kt`: Adicionar chamada de warming como fallback em `onCreate()`

### New Files to Create
- `app/src/main/java/com/example/esttufa/model/PlantClassificationResponse.kt`: DTO para a resposta de classificação
- `app/src/main/java/com/example/esttufa/repository/PlantClassificationRepository.kt`: Repository para upload de imagem e classificação
- `app/src/main/java/com/example/esttufa/warming/ApiWarmingHelper.kt`: Helper object para disparar warming fire-and-forget

---

## 4. Execution Plan & Parallelism Map
```
Phase 1 (Parallel — Modelos e Infraestrutura):
  ├── T1 [P]  (PlantClassificationResponse DTO + ApiService endpoints)
  └── T2 [P]  (ApiWarmingHelper)

Phase 2 (Sequential — Repository):
  T1 complete, then:
    T3

Phase 3 (Parallel — ViewModel + Activity Warming):
  T3 complete, then:
    ├── T4 [P]  (CulturaInfoViewModel — classifyImage)
    └── T5 [P]  (MainActivity + HomeActivity warming)

Phase 4 (Sequential — Activity Integration):
  T4 complete, then:
    T6

Phase 5 (Sequential — Verificação Final):
  T5, T6 complete, then:
    T7
```

> [!IMPORTANT]
> **Subagent Concurrency Rules**: For each task marked with `[P]`, the executing agent **MUST spawn a separate concurrent subagent** to perform the work. Sequential tasks (without `[P]`) should be executed inline or by single sequential subagents.

---

## 5. Granular Task Checklist

### T1: Criar DTO de classificação + adicionar endpoints ao ApiService [P]
- **What**: Criar o DTO `PlantClassificationResponse` para mapear a resposta da API de classificação de plantas, e adicionar os endpoints `POST /plant-classification/predict` (multipart) e `GET /hearth-beat` ao `ApiService`.
- **Where**: `app/src/main/java/com/example/esttufa/model/PlantClassificationResponse.kt` (novo), `app/src/main/java/com/example/esttufa/model/ApiService.kt` (modificar)
- **Depends on**: None
- **Reuses**: `model/IrrigationResponse.kt` como referência de DTO
- **Requirement ID**: `IMG-01`, `IMG-02`, `WARM-01`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] `PlantClassificationResponse.kt` criado com campos que mapeiam a resposta da API. Como a resposta exata não está documentada no OpenAPI schema (schema vazio `{}`), usar uma classe flexível:
    ```kotlin
    data class PlantClassificationResponse(
        val predicted_class: String?,
        val confidence: Double?,
        val class_name: String?
    )
    ```
    Nota: incluir os campos mais prováveis (`predicted_class`, `confidence`, `class_name`) com `?` (nullable) para resiliência. O campo que contiver o nome da cultura classificada será usado na UI.
  - [ ] Em `ApiService.kt`, adicionar imports: `retrofit2.http.Multipart`, `retrofit2.http.Part`, `okhttp3.MultipartBody`
  - [ ] Endpoint de classificação adicionado:
    ```kotlin
    @Multipart
    @POST("plant-classification/predict")
    suspend fun predictPlantClassification(
        @Query("model") model: String = "decision_tree",
        @Part image: MultipartBody.Part
    ): PlantClassificationResponse
    ```
  - [ ] Endpoint de healthcheck adicionado:
    ```kotlin
    @GET("hearth-beat")
    suspend fun hearthBeat()
    ```
  - [ ] Gate check: Projeto compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(api): add plant classification and hearth-beat endpoints`

---

### T2: Criar ApiWarmingHelper para pré-aquecimento [P]
- **What**: Criar um helper object que dispara uma requisição `GET /hearth-beat` em background (fire-and-forget) usando `CoroutineScope` global ou `ProcessLifecycleOwner`. O warming NÃO deve bloquear a UI nem exibir erros ao usuário.
- **Where**: `app/src/main/java/com/example/esttufa/warming/ApiWarmingHelper.kt` (novo)
- **Depends on**: None (usa apenas o `RetrofitClient` existente)
- **Reuses**: `RetrofitClient.kt`
- **Requirement ID**: `WARM-01`, `WARM-02`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] Arquivo `ApiWarmingHelper.kt` criado no pacote `com.example.esttufa.warming`
  - [ ] Object `ApiWarmingHelper` com:
    ```kotlin
    object ApiWarmingHelper {
        private val alreadyWarmed = AtomicBoolean(false)

        fun warmUp() {
            if (!alreadyWarmed.compareAndSet(false, true)) return

            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                runCatching { RetrofitClient.api.hearthBeat() }
                // Silently ignore any errors
            }
        }
    }
    ```
  - [ ] Usa `AtomicBoolean` para garantir que o warming só é disparado UMA VEZ por sessão do app, mesmo que `MainActivity` e `HomeActivity` chamem ambos
  - [ ] Erros são completamente ignorados (catch silencioso)
  - [ ] Gate check: Projeto compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(warming): add ApiWarmingHelper for Render cold-start mitigation`

---

### T3: Criar PlantClassificationRepository
- **What**: Criar repository que encapsula a chamada ao endpoint de classificação de plantas, recebendo um `MultipartBody.Part` e retornando `Result<PlantClassificationResponse>`.
- **Where**: `app/src/main/java/com/example/esttufa/repository/PlantClassificationRepository.kt` (novo)
- **Depends on**: T1
- **Reuses**: `repository/IrrigationRepository.kt` (padrão Result<T>)
- **Requirement ID**: `IMG-01`, `IMG-02`, `IMG-03`
- **Done when**:
  - [ ] Arquivo `PlantClassificationRepository.kt` criado no pacote `com.example.esttufa.repository`
  - [ ] Classe `PlantClassificationRepository` com:
    ```kotlin
    class PlantClassificationRepository(
        private val apiService: ApiService = RetrofitClient.api
    ) {
        suspend fun predict(
            imagePart: MultipartBody.Part,
            model: String = "decision_tree"
        ): Result<PlantClassificationResponse> =
            runCatching {
                apiService.predictPlantClassification(model, imagePart)
            }
    }
    ```
  - [ ] Gate check: Projeto compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(repository): add PlantClassificationRepository`

---

### T4: Expandir CulturaInfoViewModel com classificação de imagem [P]
- **What**: Adicionar ao `CulturaInfoViewModel` a capacidade de enviar uma imagem para classificação, gerenciando estados Loading/Success/Error para o fluxo de classificação.
- **Where**: `app/src/main/java/com/example/esttufa/viewmodel/CulturaInfoViewModel.kt` (modificar)
- **Depends on**: T3
- **Reuses**: Padrão sealed class UiState existente no mesmo arquivo
- **Requirement ID**: `IMG-01`, `IMG-02`, `IMG-03`, `IMG-04`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] Nova sealed class adicionada ao arquivo (separada do `CulturaInfoUiState` existente de irrigação):
    ```kotlin
    sealed class ClassificationUiState {
        object Idle : ClassificationUiState()
        object Loading : ClassificationUiState()
        data class Success(val className: String) : ClassificationUiState()
        data class Error(val message: String) : ClassificationUiState()
    }
    ```
  - [ ] Novo `LiveData` adicionado ao ViewModel:
    ```kotlin
    private val _classificationState = MutableLiveData<ClassificationUiState>(ClassificationUiState.Idle)
    val classificationState: LiveData<ClassificationUiState> = _classificationState
    ```
  - [ ] Nova dependência do repository adicionada ao constructor:
    ```kotlin
    private val classificationRepository: PlantClassificationRepository = PlantClassificationRepository()
    ```
  - [ ] Função `classifyImage(imagePart: MultipartBody.Part)` implementada:
    1. Se já está `Loading`, retorna (debounce)
    2. Seta estado para `Loading`
    3. Chama `classificationRepository.predict(imagePart)` em `viewModelScope.launch`
    4. Em `onSuccess`: extrai o nome da classe da resposta (priorizando `predicted_class`, depois `class_name`) e seta `Success(className)`
    5. Em `onFailure`: seta `Error(message)`
  - [ ] O `_uiState` de irrigação existente permanece inalterado
  - [ ] Gate check: Projeto compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(viewmodel): add image classification to CulturaInfoViewModel`

---

### T5: Integrar warming na MainActivity e HomeActivity [P]
- **What**: Chamar `ApiWarmingHelper.warmUp()` no `onCreate()` da `MainActivity` e no `onCreate()` da `HomeActivity` para pré-aquecer a API.
- **Where**: `app/src/main/java/com/example/esttufa/MainActivity.kt`, `app/src/main/java/com/example/esttufa/HomeActivity.kt`
- **Depends on**: T2
- **Reuses**: `ApiWarmingHelper` (T2)
- **Requirement ID**: `WARM-01`, `WARM-02`, `WARM-03`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] Em `MainActivity.kt`, adicionado `ApiWarmingHelper.warmUp()` no INÍCIO do `onCreate()`, ANTES de qualquer verificação de auto-login:
    ```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiWarmingHelper.warmUp() // ← adicionar aqui
        // ... resto do código existente
    }
    ```
  - [ ] Em `HomeActivity.kt`, adicionado `ApiWarmingHelper.warmUp()` no `onCreate()` como fallback:
    ```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiWarmingHelper.warmUp() // ← fallback para auto-login
        // ... resto do código existente
    }
    ```
  - [ ] O `AtomicBoolean` no `ApiWarmingHelper` garante que apenas UMA chamada é feita, mesmo que ambas Activities chamem
  - [ ] Gate check: Projeto compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(warming): integrate ApiWarmingHelper in MainActivity and HomeActivity`

---

### T6: Refatorar CulturaInfoActivity para usar API real de classificação
- **What**: Substituir a lógica fake em `processImageResult()` por chamadas reais ao ViewModel para envio da imagem à API. A imagem (Bitmap da câmera ou Uri da galeria) deve ser convertida em `MultipartBody.Part` e enviada ao ViewModel. A Activity deve observar o novo `classificationState` do ViewModel.
- **Where**: `app/src/main/java/com/example/esttufa/CulturaInfoActivity.kt` (modificar)
- **Depends on**: T4
- **Reuses**: Layout existente (`cvFotoReconhecida`, `cvFotoNaoReconhecida`, `tvNomeCulturaResult`, `ivFotoResult`, `tvInstrucaoFoto`, `progressBar`)
- **Requirement ID**: `IMG-01`, `IMG-02`, `IMG-03`, `IMG-04`, `IMG-05`
- **Done when**:
  - [ ] Import de `okhttp3.MultipartBody`, `okhttp3.RequestBody.Companion.toRequestBody`, `okhttp3.MediaType.Companion.toMediaType` adicionados
  - [ ] Método `processImageResult(bitmap: Bitmap?, uri: Uri?)` refatorado:
    1. **REMOVER** a linha `val isRecognized = Random().nextInt(10) < 7` e toda a lógica de randomização
    2. Ocultar `tvInstrucaoFoto`
    3. Exibir a imagem capturada/selecionada no `ivFotoResult` imediatamente (como feedback visual)
    4. Converter a imagem para bytes:
       - Se `bitmap != null`: comprimir para JPEG via `bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)` e obter `ByteArray`
       - Se `uri != null`: abrir `contentResolver.openInputStream(uri)` e ler os bytes
    5. Criar `MultipartBody.Part`:
       ```kotlin
       val requestBody = imageBytes.toRequestBody("image/*".toMediaType())
       val part = MultipartBody.Part.createFormData("image", "photo.jpg", requestBody)
       ```
    6. Chamar `viewModel.classifyImage(part)`
  - [ ] Novo observer adicionado em `observeViewModel()` para `viewModel.classificationState`:
    - `Idle`: nenhuma ação
    - `Loading`: exibir `progressBar`, ocultar ambos os cards de resultado
    - `Success`:
      - Ocultar `progressBar`
      - Exibir `cvFotoReconhecida` com `tvNomeCulturaResult.text = state.className`
      - Ocultar `cvFotoNaoReconhecida`
      - Exibir a imagem no `ivFotoResult` (já feito no processImageResult)
    - `Error`:
      - Ocultar `progressBar`
      - Exibir `cvFotoNaoReconhecida`
      - Ocultar `cvFotoReconhecida`
  - [ ] O `fabCamera` deve ser desabilitado enquanto a classificação está em `Loading` para evitar envios duplicados
  - [ ] Remover import de `java.util.Random` (ou `java.util.*` se não for mais necessário por outro uso — verificar se `java.util.Locale` é usado em `updateTimerUI()`, nesse caso manter `java.util.*` ou ajustar imports)
  - [ ] Gate check: Projeto compila sem erros
- **Tests**: none
- **Gate**: quick
- **Commit**: `fix(cultura-info): use real API for plant classification instead of Random()`

---

### T7: Verificação final e smoke test
- **What**: Executar verificação completa end-to-end dos dois novos comportamentos: classificação de imagem via API e warming da API.
- **Where**: Projeto completo
- **Depends on**: T5, T6
- **Reuses**: None
- **Requirement ID**: Todos (`IMG-01` a `IMG-05`, `WARM-01` a `WARM-03`)
- **Done when**:
  - [ ] Projeto compila sem erros: `./gradlew assembleDebug`
  - [ ] **Warming**: ao abrir o app (login ou auto-login), a requisição `GET /hearth-beat` aparece nos logs do OkHttp
  - [ ] **Warming**: se a API estava dormindo, a resposta do healthcheck acorda o serviço para as requisições subsequentes serem mais rápidas
  - [ ] **Warming**: se a chamada falha (sem rede, timeout), nenhum crash ou Toast é exibido
  - [ ] **Classificação**: ao tirar foto na `CulturaInfoActivity`, a imagem é enviada via `POST /plant-classification/predict` (verificar nos logs OkHttp que o multipart é enviado)
  - [ ] **Classificação**: ao receber sucesso da API, o card `cvFotoReconhecida` exibe a imagem e o nome da classe retornado pela API
  - [ ] **Classificação**: ao receber erro (simular desligando rede), o card `cvFotoNaoReconhecida` é exibido
  - [ ] **Classificação**: durante o loading, o `progressBar` é exibido e o botão de câmera é desabilitado
  - [ ] **Classificação**: a lógica de `Random()` foi completamente removida — o resultado depende 100% da API
  - [ ] Nenhuma referência a `Random().nextInt` permanece em `CulturaInfoActivity.kt`
  - [ ] O fluxo de irrigação existente (sensores, timer, popup) continua funcionando normalmente sem regressões
- **Tests**: Manual smoke test completo contra `https://api-esttufa.onrender.com/`
- **Gate**: full
- **Commit**: nenhum (apenas verificação)
