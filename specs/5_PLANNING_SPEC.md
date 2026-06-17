# Planning Spec: Implementação de Persistência Local e Sensores
**Status**: PROCESSING
**Priority**: high

> [!IMPORTANT]
> **Instructions for the Executing AI**:
> You MUST update the `Status` field above as you progress through this implementation:
> - **TO DO**: The specification is created but execution has not started.
> - **PROCESSING**: Update to this status as soon as you begin work on the first task.
> - **DONE**: Update to this status when all tasks are fully completed and verified.
> - **BUG**: take this spec again.

## 1. Project Context & Stack
- **Languages & Runtime**: Kotlin, Android SDK 34-36.
- **Core Frameworks**: Android UI Toolkit, Retrofit, Coroutines.
- **Testing Infrastructure**: JUnit, Espresso.
- **Observed Conventions & Patterns**: MVVM leve, Repositories, ViewBinding.
- **Risks & Fragile Areas**: Dependências do Room, Maps e Sensor Manager precisarão ser injetadas ou configuradas na Activity/ViewModel correspondente de forma reativa.

## 2. Requirements & Acceptance Criteria
- **Goal**: Adicionar requisitos faltantes de armazenamento local (Room, SharedPreferences), APIs de mapas (Google Maps) e sensores físicos (Acelerômetro), mantendo a performance com Background Threading.
- **Acceptance Criteria**:
  - `REQ-01`: WHEN the user saves a sensor reading THEN the system SHALL save it offline using Room Database running on `Dispatchers.IO`.
  - `REQ-02`: WHEN the user changes the temperature unit or theme in Profile THEN the system SHALL save this in SharedPreferences.
  - `REQ-03`: WHEN the user creates a new stove (CadastroEstufa) THEN the system SHALL show a Map to pick the latitude and longitude.
  - `REQ-04`: WHEN the user shakes the phone in HomeActivity THEN the system SHALL detect it via Accelerometer and refresh the stoves list from the API using `Dispatchers.IO`.

## 3. Design & Impact Analysis
- **Code Reuse Blueprints**: 
  - `repository/StoveRepository.kt` can be used as a pattern for new Local Repositories.
- **Files to Modify**:
  - `app/build.gradle.kts`: Add Room, Google Maps SDK dependencies.
  - `app/src/main/java/com/example/esttufa/HomeActivity.kt`: Add Accelerometer SensorEventListener.
  - `app/src/main/java/com/example/esttufa/CadastroEstufaActivity.kt`: Add MapView or MapFragment.
  - `app/src/main/java/com/example/esttufa/ProfileActivity.kt`: Add settings toggles and SharedPreferences logic.
- **New Files to Create**:
  - `app/src/main/java/com/example/esttufa/local/RoomAppDatabase.kt`: Room database config.
  - `app/src/main/java/com/example/esttufa/local/SensorReadingDao.kt` & `app/src/main/java/com/example/esttufa/local/SensorReadingEntity.kt`: Room entities and DAOs.
  - `app/src/main/java/com/example/esttufa/local/PreferencesHelper.kt`: Helper for SharedPreferences.

## 4. Execution Plan & Parallelism Map
```
Phase 1 (Sequential - Setup & Gradle):
  T1 (Dependencies Setup) ──→ T2 (Local DB Setup)

Phase 2 (Parallel - Implementations):
  T2 complete, then:
    ├── T3 [P] (Room Sensors & Coroutines IO)
    ├── T4 [P] (SharedPreferences Profile)
    ├── T5 [P] (Map Integration on CadastroEstufa)
    └── T6 [P] (Accelerometer on Home)
```

> [!IMPORTANT]
> **Subagent Concurrency Rules**: For each task marked with `[P]`, the executing agent **MUST spawn a separate concurrent subagent** to perform the work. Sequential tasks (without `[P]`) should be executed inline or by single sequential subagents.

## 5. Granular Task Checklist

### T1: Setup Dependencies
- **What**: Adicionar dependências do Room, Google Maps SDK (play-services-maps) no `app/build.gradle.kts`.
- **Where**: `app/build.gradle.kts`
- **Depends on**: None
- **Reuses**: None
- **Requirement ID**: `REQ-01`, `REQ-03`
- **Done when**:
  - [x] Dependências adicionadas com sucesso.
  - [x] Gate check passes: `./gradlew assembleDebug`
  - [x] Test count: 0
- **Tests**: none
- **Gate**: quick
- **Commit**: `chore(build): add Room and Maps dependencies`

### T2: Configurar Room Database e Entidades
- **What**: Criar a entidade e DAO de leitura de sensores.
- **Where**: `app/src/main/java/com/example/esttufa/local/RoomAppDatabase.kt`
- **Depends on**: T1
- **Reuses**: None
- **Requirement ID**: `REQ-01`
- **Done when**:
  - [ ] Database e classes criadas.
  - [ ] Gate check passes: `./gradlew assembleDebug`
  - [ ] Test count: 0
- **Tests**: unit
- **Gate**: quick
- **Commit**: `feat(db): create Room database for sensor readings`

### T3: Coroutines IO e Integração Room [P]
- **What**: Inserir leituras do Room sempre chamando Coroutines `Dispatchers.IO`.
- **Where**: `app/src/main/java/com/example/esttufa/repository/SensorLocalRepository.kt`
- **Depends on**: T2
- **Reuses**: None
- **Requirement ID**: `REQ-01`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] Repositório local de sensores criado e usando `withContext(Dispatchers.IO)`.
  - [ ] Gate check passes: `./gradlew assembleDebug`
  - [ ] Test count: 0
- **Tests**: unit
- **Gate**: quick
- **Commit**: `feat(db): implement local repository with Coroutines IO`

### T4: SharedPreferences no Profile [P]
- **What**: Adicionar armazenamento e leitura de Preferências de Tema e Unidade (C/F) usando SharedPreferences no Profile.
- **Where**: `app/src/main/java/com/example/esttufa/ProfileActivity.kt`
- **Depends on**: T2
- **Reuses**: None
- **Requirement ID**: `REQ-02`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] SharedPreferences utilizado e preferências aplicadas.
  - [ ] Gate check passes: `./gradlew assembleDebug`
  - [ ] Test count: 0
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(profile): save theme and unit preferences locally`

### T5: Integração Google Maps no Cadastro [P]
- **What**: Incluir `MapView` na interface do Cadastro de Estufa.
- **Where**: `app/src/main/java/com/example/esttufa/CadastroEstufaActivity.kt`
- **Depends on**: T2
- **Reuses**: None
- **Requirement ID**: `REQ-03`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] Mapa adicionado e marcando a coordenada selecionada.
  - [ ] Gate check passes: `./gradlew assembleDebug`
  - [ ] Test count: 0
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(map): add Google Maps to Stove creation screen`

### T6: Acelerômetro para Refresh [P]
- **What**: Capturar eventos do acelerômetro na HomeActivity para forçar recarregamento.
- **Where**: `app/src/main/java/com/example/esttufa/HomeActivity.kt`
- **Depends on**: T2
- **Reuses**: None
- **Requirement ID**: `REQ-04`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] SensorManager configurado e detectando shake.
  - [ ] Lista é atualizada utilizando `Dispatchers.IO`.
  - [ ] Gate check passes: `./gradlew assembleDebug`
  - [ ] Test count: 0
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(sensor): add shake to refresh using Accelerometer`
