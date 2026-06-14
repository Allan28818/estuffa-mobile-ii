# Planning Spec: Recuperação de Senha (Esqueceu a senha)
**Status**: PROCESSING
**Started at**: 2026-06-14 16:29
**Priority**: high

> [!IMPORTANT]
> **Instructions for the Executing AI**:
> You MUST update the `Status` field above as you progress through this implementation:
> - **TO DO**: The specification is created but execution has not started.
> - **PROCESSING**: Update to this status as soon as you begin work on the first task. You must also record the date and time when this status is set. Note: Specs marked as PROCESSING can be picked up again if they have been in this status for more than 1 hour (the executing AI already knows how to verify this).
> - **DONE**: Update to this status when all tasks are fully completed and verified.
> - **BUG**: take this spec again. It's not working and need to be tested. If possible, ask to the user to discover which problems this spec has. Ensure that the reason for the bug is clearly documented so the AI understands exactly what to fix.
> Do not begin implementation without updating the status to `PROCESSING`.

## 1. Project Context & Stack
- **Languages & Runtime**: Kotlin, Android SDK (minSdk 34, targetSdk 36)
- **Core Frameworks**: Android Views (ViewBinding), Firebase Auth, Coroutines/LiveData, Material Components
- **Architecture**: MVVM-ish (Activity + ViewModel with UiState sealed classes)
- **Observed Conventions & Patterns**:
  - `ViewModel` uses `MutableLiveData` and a sealed class for UI state (`LoginUiState` style).
  - UI binds via `ActivityMainBinding.inflate`.
  - Firebase calls are made directly in the `ViewModel` (e.g., `FirebaseAuth.getInstance()`).
  - Validation done in the Activity before calling the ViewModel.

## 2. Requirements & Acceptance Criteria
- **Goal**: Implement the "Forgot Password" feature allowing users to request a password reset email via Firebase Auth.
- **Acceptance Criteria**:
  - `RECSENHA-01`: WHEN the user clicks on "Esqueci a senha" in the login screen, THEN the system SHALL navigate to the `EsqueciSenhaActivity`.
  - `RECSENHA-02`: WHEN the user enters an invalid or empty email and submits, THEN the system SHALL show a validation error on the input field.
  - `RECSENHA-03`: WHEN the user submits a valid email, THEN the system SHALL call Firebase Auth `sendPasswordResetEmail`.
  - `RECSENHA-04`: WHEN the Firebase request is loading, THEN the system SHALL disable the submit button (and optionally show a loading state).
  - `RECSENHA-05`: WHEN the Firebase request succeeds, THEN the system SHALL show a success message (Toast) and navigate back to the login screen.
  - `RECSENHA-06`: WHEN the Firebase request fails, THEN the system SHALL show a readable error message (Toast) to the user.
  - `RECSENHA-07`: WHEN the password reset email is sent, THEN it SHALL use the custom HTML template provided and the title "Recupere a sua senha do Esttufa 🌱".

## 3. Design & Impact Analysis
- **Code Reuse Blueprints**:
  - `LoginViewModel`: Use as a blueprint for `EsqueciSenhaViewModel` and its `UiState`.
  - `ActivityMainBinding`: Use as a blueprint for the layout structure (Material TextInputLayout).
- **Files to Modify**:
  - `app/src/main/java/com/example/esttufa/MainActivity.kt`: Add click listener to `binding.tvEsqueciSenha` to start `EsqueciSenhaActivity`.
  - `app/src/main/AndroidManifest.xml`: Register `EsqueciSenhaActivity`.
- **New Files to Create**:
  - `app/src/main/res/layout/activity_esqueci_senha.xml`: Layout for the forgot password screen (Email input + Submit button).
  - `app/src/main/java/com/example/esttufa/viewmodel/EsqueciSenhaViewModel.kt`: ViewModel to handle `sendPasswordResetEmail`.
  - `app/src/main/java/com/example/esttufa/EsqueciSenhaActivity.kt`: Activity that binds the layout and observes the ViewModel.

## 4. Execution Plan & Parallelism Map
Visual map of execution phases:

Phase 1 (Parallel):
  ├── T1 [P] (Create Layout & Activity shell & Manifest)
  ├── T2 [P] (Create ViewModel & UiState)
  └── T4 [P] (Configure Firebase Auth Email Template)

Phase 2 (Sequential):
  T1, T2 complete, then:
    T3 (Integrate Activity with ViewModel & MainActivity Navigation)

> [!IMPORTANT]
> **Subagent Concurrency Rules**: For each task marked with `[P]`, the executing agent **MUST spawn a separate concurrent subagent** to perform the work. Sequential tasks (without `[P]`) should be executed inline or by single sequential subagents.

## 5. Granular Task Checklist

### T1: Create Layout and Activity Definition [P]
- **What**: Create the layout XML for the "Esqueci a senha" screen, the Activity class skeleton, and declare it in the AndroidManifest.
- **Where**:
  - `app/src/main/res/layout/activity_esqueci_senha.xml`
  - `app/src/main/java/com/example/esttufa/EsqueciSenhaActivity.kt`
  - `app/src/main/AndroidManifest.xml`
- **Depends on**: None
- **Reuses**: `activity_main.xml` (for styling references)
- **Requirement ID**: `RECSENHA-01`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] Layout file exists with a `TextInputEditText` for email and a `MaterialButton` for submission.
  - [ ] `EsqueciSenhaActivity` class exists and inflates the layout using ViewBinding.
  - [ ] Activity is registered in `AndroidManifest.xml`.
  - [ ] Gate check passes: `./gradlew assembleDebug`
- **Tests**: none
- **Gate**: quick
- **Commit**: `feat(auth): create forgot password UI layout and activity skeleton`

### T2: Create ViewModel and UiState [P]
- **What**: Create `EsqueciSenhaViewModel` with its corresponding UI State to handle the Firebase password reset call.
- **Where**: `app/src/main/java/com/example/esttufa/viewmodel/EsqueciSenhaViewModel.kt`
- **Depends on**: None
- **Reuses**: `LoginViewModel.kt`
- **Requirement ID**: `RECSENHA-03`, `RECSENHA-04`, `RECSENHA-06`
- **Execution**: Spawn concurrent subagent 🚀
- **Done when**:
  - [ ] `EsqueciSenhaUiState` sealed class is defined (Idle, Loading, Success, Error).
  - [ ] ViewModel is created and exposes a `LiveData<EsqueciSenhaUiState>`.
  - [ ] ViewModel has a `sendResetEmail(email: String)` function that calls `FirebaseAuth.getInstance().sendPasswordResetEmail(email)`.
  - [ ] Exceptions are treated properly (e.g., `FirebaseAuthInvalidUserException`).
  - [ ] Gate check passes: `./gradlew assembleDebug`
- **Tests**: none (Firebase wrapper logic)
- **Gate**: quick
- **Commit**: `feat(auth): create viewmodel for password reset`

### T3: Integration and Navigation
- **What**: Connect the ViewModel to the Activity, add validation logic, and wire up `MainActivity` to navigate to the new screen.
- **Where**:
  - `app/src/main/java/com/example/esttufa/EsqueciSenhaActivity.kt`
  - `app/src/main/java/com/example/esttufa/MainActivity.kt`
- **Depends on**: T1, T2
- **Reuses**: None
- **Requirement ID**: `RECSENHA-01`, `RECSENHA-02`, `RECSENHA-05`
- **Done when**:
  - [ ] `MainActivity` sets a click listener on `tvEsqueciSenha` that starts `EsqueciSenhaActivity`.
  - [ ] `EsqueciSenhaActivity` validates the email input before calling `viewModel.sendResetEmail`.
  - [ ] `EsqueciSenhaActivity` observes the ViewModel state (shows Toast on error/success, finish() on success, handles loading state on the button).
  - [ ] Gate check passes: `./gradlew assembleDebug`
- **Tests**: manual UI testing
- **Gate**: full
- **Commit**: `feat(auth): integrate forgot password flow and navigation`

### T4: Configure Firebase Auth Email Template [P]
- **What**: Configure the "Password Reset" email template in the Firebase Console to use the provided Title and custom HTML template. *(Note: By default, Firebase limits HTML customization on its native templates. If Firebase native templates don't allow this full HTML injection, the executor must document the need for a Firebase Extension like 'Trigger Email from Firestore' or a custom Cloud Function to send this HTML via SendGrid/SMTP).*
- **Where**: Firebase Console -> Authentication -> Templates -> Password Reset (ou Cloud Functions)
- **Depends on**: None
- **Reuses**: None
- **Requirement ID**: `RECSENHA-07`
- **Execution**: Spawn concurrent subagent 🚀 (ou instruir intervenção manual do usuário no console Firebase)
- **Done when**:
  - [ ] O assunto do e-mail está configurado para: `"Recupere a sua senha do Esttufa 🌱"`
  - [ ] O código HTML do e-mail está configurado utilizando o template abaixo (com a tag `%LINK%` substituída corretamente pelo link de reset do Firebase).
- **Tests**: manual (solicitar e-mail e checar caixa de entrada)
- **Gate**: quick
- **Commit**: `chore(firebase): apply custom email template for password reset`

<details>
<summary><b>HTML Template</b></summary>

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
  <title>Redefinição de Senha — Esttufa</title>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet"/>
  <!--[if mso]>
  <noscript>
    <xml>
      <o:OfficeDocumentSettings>
        <o:PixelsPerInch>96</o:PixelsPerInch>
      </o:OfficeDocumentSettings>
    </xml>
  </noscript>
  <![endif]-->
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: 'Inter', Arial, sans-serif;
      background-color: #F4F8FA;
      color: #1E2A38;
      -webkit-font-smoothing: antialiased;
    }
    .email-wrapper {
      width: 100%;
      background-color: #F4F8FA;
      padding: 48px 16px;
    }
    .email-container {
      max-width: 560px;
      margin: 0 auto;
      background-color: #FFFFFF;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 2px 16px rgba(30, 42, 56, 0.08);
    }

    /* Header */
    .email-header {
      background-color: #059162;
      padding: 36px 48px 32px;
      text-align: left;
      position: relative;
    }
    .email-header::after {
      content: '';
      display: block;
      position: absolute;
      bottom: 0;
      right: 0;
      width: 120px;
      height: 120px;
      background-color: #1C7054;
      border-radius: 50% 0 0 0;
      opacity: 0.5;
    }
    .logo-mark {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      position: relative;
      z-index: 1;
    }
    .logo-icon {
      width: 36px;
      height: 36px;
      background-color: #FFFFFF;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .logo-icon svg {
      width: 22px;
      height: 22px;
    }
    .logo-text {
      font-size: 22px;
      font-weight: 700;
      color: #FFFFFF;
      letter-spacing: -0.5px;
    }
    .logo-text span {
      color: #FF793F;
    }

    /* Body */
    .email-body {
      padding: 48px 48px 40px;
    }
    .tag-line {
      display: inline-block;
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 1.2px;
      text-transform: uppercase;
      color: #059162;
      margin-bottom: 16px;
    }
    .email-title {
      font-size: 26px;
      font-weight: 700;
      color: #1E2A38;
      line-height: 1.25;
      margin-bottom: 16px;
      letter-spacing: -0.4px;
    }
    .email-subtitle {
      font-size: 15px;
      font-weight: 400;
      color: #243447;
      line-height: 1.65;
      margin-bottom: 36px;
      opacity: 0.8;
    }

    /* Divider */
    .divider {
      width: 40px;
      height: 3px;
      background: linear-gradient(90deg, #059162, #FF793F);
      border-radius: 2px;
      margin-bottom: 32px;
    }

    /* CTA Button */
    .cta-wrapper {
      margin-bottom: 36px;
    }
    .cta-button {
      display: inline-block;
      background-color: #059162;
      color: #FFFFFF !important;
      font-family: 'Inter', Arial, sans-serif;
      font-size: 15px;
      font-weight: 600;
      text-decoration: none;
      padding: 15px 36px;
      border-radius: 8px;
      letter-spacing: 0.1px;
      transition: background-color 0.2s;
    }
    .cta-button:hover {
      background-color: #1C7054;
    }

    /* Fallback link */
    .fallback-section {
      background-color: #F4F8FA;
      border-radius: 8px;
      padding: 20px 24px;
      margin-bottom: 36px;
    }
    .fallback-label {
      font-size: 12px;
      font-weight: 600;
      color: #059162;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      margin-bottom: 8px;
    }
    .fallback-url {
      font-size: 12px;
      color: #243447;
      word-break: break-all;
      line-height: 1.6;
      opacity: 0.7;
    }

    /* Warning */
    .warning-block {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      border-left: 3px solid #FF793F;
      padding-left: 16px;
      margin-bottom: 40px;
    }
    .warning-text {
      font-size: 13px;
      color: #243447;
      line-height: 1.6;
      opacity: 0.75;
    }
    .warning-text strong {
      color: #1E2A38;
      opacity: 1;
    }

    /* Footer */
    .email-footer {
      background-color: #1E2A38;
      padding: 28px 48px;
      text-align: center;
    }
    .footer-logo {
      font-size: 14px;
      font-weight: 700;
      color: #FFFFFF;
      letter-spacing: -0.2px;
      margin-bottom: 10px;
    }
    .footer-logo span {
      color: #FF793F;
    }
    .footer-text {
      font-size: 12px;
      color: #FFFFFF;
      opacity: 0.4;
      line-height: 1.6;
    }
    .footer-text a {
      color: #059162;
      text-decoration: none;
    }

    @media only screen and (max-width: 600px) {
      .email-header { padding: 28px 28px 24px; }
      .email-body { padding: 36px 28px 32px; }
      .email-footer { padding: 24px 28px; }
      .email-title { font-size: 22px; }
      .cta-button { display: block; text-align: center; }
    }
  </style>
</head>
<body>
  <div class="email-wrapper">
    <div class="email-container">

      <!-- Header -->
      <div class="email-header">
        <div class="logo-mark">
          <div class="logo-icon">
            <!-- Ícone de folha/planta estilizado -->
            <svg viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M11 19C11 19 4 14 4 8.5C4 5.46 6.69 3 10 3C10.68 3 11.34 3.11 11.96 3.31C12.58 3.11 13.32 3 14 3C17.31 3 20 5.46 20 8.5C20 14 13 19 11 19Z" fill="#059162"/>
              <path d="M11 19V10" stroke="#FFFFFF" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <span class="logo-text">Est<span>t</span>ufa</span>
        </div>
      </div>

      <!-- Body -->
      <div class="email-body">
        <span class="tag-line">Segurança da conta</span>
        <h1 class="email-title">Redefinição de<br/>senha solicitada</h1>
        <div class="divider"></div>
        <p class="email-subtitle">
          Recebemos uma solicitação para redefinir a senha da sua conta na Esttufa. Clique no botão abaixo para criar uma nova senha segura.
        </p>

        <!-- CTA -->
        <div class="cta-wrapper">
          <a href="%LINK%" class="cta-button">Redefinir minha senha</a>
        </div>

        <!-- Fallback link -->
        <div class="fallback-section">
          <p class="fallback-label">Botão não funciona?</p>
          <p class="fallback-url">Cole este link no seu navegador:<br/>%LINK%</p>
        </div>

        <!-- Warning -->
        <div class="warning-block">
          <div>
            <p class="warning-text">
              <strong>Este link expira em 1 hora.</strong> Se você não solicitou a redefinição de senha, ignore este e-mail — sua conta está segura e nenhuma alteração será feita.
            </p>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="email-footer">
        <p class="footer-logo">Est<span>t</span>ufa</p>
        <p class="footer-text">
          Você está recebendo este e-mail pois uma solicitação foi feita para a conta associada a este endereço.<br/>
          <a href="mailto:suporte@esttufa.com">suporte@esttufa.com</a>
        </p>
      </div>

    </div>
  </div>
</body>
</html>
```
</details>

## 6. Execution Notes

- T1, T2 and T3 were implemented and validated on 2026-06-14.
- `gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain`
  completed successfully.
- T4 remains external: the native Identity Platform template supports a custom
  subject, HTML body and `%LINK%`, but this repository has no Firebase
  Functions, Extensions or authenticated project configuration.
- Apply the subject `Recupere a sua senha do Esttufa 🌱` and the HTML above in
  Firebase Console -> Authentication -> Templates -> Password reset.
- After saving, request a reset for an existing account and verify the received
  message and action link. Only then may this spec be changed to `DONE`.
