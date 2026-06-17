# Esttufa Mobile

## Visão Geral

O **Esttufa Mobile** é um aplicativo Android para gestão e monitoramento de
estufas inteligentes. O projeto une agricultura urbana, tecnologia móvel,
automação e recursos nativos do Android para criar uma experiência em que o
usuário pode cadastrar estufas, acompanhar culturas, consultar dados simulados
de sensores, calcular irrigação, escolher localização no mapa e classificar
plantas por imagem.

Este README foi escrito para apoiar a apresentação em vídeo do projeto. O foco
está no tema, nas funcionalidades entregues e na forma como os requisitos
mínimos são atendidos dentro do aplicativo.

## Tema do Projeto

A proposta do Esttufa é representar um sistema móvel de apoio ao cultivo em
estufas conectadas. O usuário acompanha culturas como alface, tomate e rúcula,
consulta informações de temperatura, umidade, luminosidade e irrigação, registra
dados localmente e usa recursos do celular para enriquecer a gestão da estufa.

Durante a apresentação, o ponto principal é mostrar que o app não é apenas um
conjunto de telas: ele integra autenticação, API REST, persistência local,
sensores, mapa, câmera, galeria e processamento em background.

## Funcionalidades do Aplicativo

- Login, cadastro de usuário, auto-login, logout e recuperação de senha com
  Firebase Authentication.
- Cadastro de estufas com nome, cultura e seleção visual de coordenadas no mapa.
- Listagem das estufas do usuário autenticado.
- Tela de detalhes da cultura com temperatura, umidade, luminosidade e tempo de
  irrigação.
- Consulta de dados de irrigação em uma API REST.
- Classificação de plantas usando imagem da câmera ou da galeria.
- Envio de imagem para a API por multipart.
- Persistência local de leituras de sensores usando Room.
- Preferências locais de tema e unidade de temperatura usando
  SharedPreferences.
- Atualização da Home por gesto de shake com o acelerômetro.
- Perfil do usuário com informações de assinatura e atalhos de configuração.
- Catálogo de planos com cards expansíveis e confirmação em bottom sheet.

## Roteiro Sugerido para o Vídeo

1. Apresentar o Esttufa como aplicativo de gestão de estufas inteligentes.
2. Mostrar login, cadastro e recuperação de senha.
3. Entrar na Home e explicar a listagem das estufas vindas da API.
4. Criar uma nova estufa, escolhendo cultura e localização no mapa.
5. Abrir os detalhes da cultura e demonstrar sensores, irrigação e
   classificação por imagem.
6. Mostrar o perfil, as preferências locais e os planos do serviço.
7. Explicar a arquitetura MVVM e apontar onde aparecem API REST, Room,
   SharedPreferences, segunda thread, testes, acelerômetro e mapas.

## Arquitetura MVVM

O projeto aplica o padrão **MVVM**, adequado para aplicativos móveis com várias
telas, estados de interface e integrações externas.

```text
Activity -> ViewModel -> Repository -> Retrofit / Room / SharedPreferences
```

- **View**: as Activities e os layouts XML exibem informações, capturam ações do
  usuário e observam mudanças de estado.
- **ViewModel**: concentra estados de tela, validações, chamadas assíncronas e
  comunicação com os repositories.
- **Repository**: isola acesso à API, persistência local, dados do perfil e
  catálogos usados pelo aplicativo.
- **Model e Local**: agrupam DTOs, contratos de rede, banco Room e preferências
  locais.

Essa divisão facilita a manutenção e deixa claro, durante o vídeo, que a tela
não acessa diretamente banco, API ou detalhes de autenticação.

## Telas Desenvolvidas

O requisito de no mínimo cinco telas é atendido e superado. O aplicativo possui
oito telas principais:

| Tela | Arquivo | O que demonstrar |
| --- | --- | --- |
| Login | `MainActivity.kt` | Entrada do usuário e auto-login. |
| Cadastro | `CadastroActivity.kt` | Criação de conta com validação. |
| Recuperação de senha | `EsqueciSenhaActivity.kt` | Redefinição de senha pelo Firebase. |
| Home | `HomeActivity.kt` | Lista de estufas e atualização por shake. |
| Cadastro de estufa | `CadastroEstufaActivity.kt` | Formulário, cultura e mapa. |
| Detalhes da cultura | `CulturaInfoActivity.kt` | Sensores, irrigação, câmera e classificação. |
| Perfil | `ProfileActivity.kt` | Dados do usuário e preferências locais. |
| Planos | `PlansActivity.kt` | Catálogo de planos e confirmação de assinatura. |

## Atendimento dos Requisitos Mínimos

| Requisito | Como o Esttufa atende | Arquivos principais |
| --- | --- | --- |
| Arquitetura de software móvel com MVVM | Activities observam estados de ViewModels; ViewModels acionam repositories; repositories isolam rede e persistência. | `viewmodel/*.kt`, `repository/*.kt`, `MainActivity.kt`, `HomeActivity.kt` |
| Desenvolver no mínimo 5 telas | O app possui 8 telas principais registradas no Manifest. | `AndroidManifest.xml`, `activity_*.xml` |
| Realizar requisições a uma API REST | Retrofit consome a API `https://api-esttufa.onrender.com/` para estufas, irrigação, classificação de plantas e healthcheck. | `RetrofitClient.kt`, `ApiService.kt`, `StoveRepository.kt`, `IrrigationRepository.kt`, `PlantClassificationRepository.kt` |
| Persistir dados usando Room | Leituras de sensores são salvas localmente na tabela `sensor_readings`. | `RoomAppDatabase.kt`, `SensorReadingEntity.kt`, `SensorReadingDao.kt`, `SensorLocalRepository.kt` |
| Utilizar SharedPreferences | Tema e unidade de temperatura são salvos como preferências locais do perfil. | `PreferencesHelper.kt`, `ProfileActivity.kt` |
| Executar processamento em segunda thread | Coroutines usam `viewModelScope`, `lifecycleScope` e `Dispatchers.IO` para rede, banco local, imagem e aquecimento da API. | `HomeViewModel.kt`, `SensorLocalRepository.kt`, `ApiWarmingHelper.kt`, `ProfileActivity.kt`, `CulturaInfoActivity.kt` |
| Implementar testes unitários | Testes JUnit validam a resposta de classificação de plantas e compatibilidade com campos alternativos da API. | `PlantClassificationResponseTest.kt`, `ExampleUnitTest.kt` |
| Fazer uso do sensor acelerômetro | A Home detecta shake para atualizar a lista de estufas; a tela de cultura também registra o acelerômetro. | `HomeActivity.kt`, `CulturaInfoActivity.kt` |
| Integrar mapas | O cadastro de estufa usa Google Maps para selecionar coordenadas com marcador. | `CadastroEstufaActivity.kt`, `activity_cadastro_estufa.xml`, `AndroidManifest.xml` |

## Integrações Técnicas

- **Firebase Authentication**: autenticação, criação de conta, sessão atual,
  recuperação de senha e logout.
- **Retrofit e OkHttp**: comunicação com a API REST e envio de token Bearer nas
  rotas autenticadas.
- **Room**: banco local para armazenar leituras de sensores.
- **SharedPreferences**: preferências simples de perfil.
- **Google Maps SDK**: exibição de mapa e seleção de coordenadas.
- **Acelerômetro**: interação por movimento para atualizar a Home.
- **Câmera e galeria**: captura e seleção de imagens para classificação.
- **Coroutines**: execução de tarefas fora da thread principal.

## Estrutura do Código

```text
app/src/main/java/com/example/esttufa/
|-- MainActivity.kt
|-- CadastroActivity.kt
|-- EsqueciSenhaActivity.kt
|-- HomeActivity.kt
|-- CadastroEstufaActivity.kt
|-- CulturaInfoActivity.kt
|-- ProfileActivity.kt
|-- PlansActivity.kt
|-- viewmodel/
|-- repository/
|-- model/
|-- local/
|-- adapter/
|-- auth/
`-- warming/
```

## Demonstração Recomendada

Uma boa sequência para o vídeo é:

1. Fazer login ou criar uma conta.
2. Mostrar a Home com a lista de estufas.
3. Sacudir o celular ou emulador para demonstrar o acelerômetro.
4. Cadastrar uma estufa e selecionar um ponto no mapa.
5. Abrir uma cultura e consultar dados de irrigação.
6. Selecionar uma imagem ou tirar uma foto para classificar a planta.
7. Mostrar o perfil, alterar preferências locais e abrir os planos.
8. Finalizar explicando a tabela de requisitos mínimos.

## Validação

O projeto possui validação registrada com Gradle:

```text
gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain
BUILD SUCCESSFUL em 2026-06-17
```

Essa validação cobre testes unitários, lint e geração do APK de debug.

## Pontos Fortes para Destacar

- O tema de estufa inteligente aparece em todo o fluxo: cadastro, cultura,
  sensores, irrigação, localização e classificação de plantas.
- O app usa recursos reais de desenvolvimento Android, não apenas telas
  estáticas.
- A arquitetura MVVM separa interface, estado, regras de apresentação,
  integração com API e persistência local.
- O projeto atende todos os requisitos mínimos solicitados e ainda inclui
  recursos adicionais como Firebase, câmera, galeria, planos e bottom sheet.

