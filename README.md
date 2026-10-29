# 🌊 Aqua Fit

App Android para montar e registrar treinos de calistenia. Você monta seus treinos, executa marcando série por série e acompanha o histórico — tudo offline, salvo no próprio celular.

[![Baixar APK](https://img.shields.io/badge/baixar-APK-00aaaa?logo=android&logoColor=white)](https://github.com/MatheusTogni/AquaFit/releases/latest)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3ddc84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7f52ff?logo=kotlin&logoColor=white)
![Sem dependências](https://img.shields.io/badge/depend%C3%AAncias-0-blue)

<!--
  Capturas de tela: salve as imagens em docs/ e descomente o bloco abaixo.
<p align="center">
  <img src="docs/treinos.png" width="230" alt="Lista de treinos">
  <img src="docs/treino.png" width="230" alt="Treino em andamento">
  <img src="docs/historico.png" width="230" alt="Histórico">
</p>
-->

## Funcionalidades

- **Treinos e exercícios** — cada exercício tem séries, repetições *ou* tempo (isometria), carga em kg (negativa para assistência com elástico) e observações.
- **Gestos nos cards** — tocar abre, deslizar revela editar/excluir e segurar e arrastar reordena, com a lista rolando sozinha perto das bordas.
- **Execução do treino** — cronômetro, contador de séries feitas e ajuste de reps/carga série a série; a tela fica ligada enquanto o treino está aberto.
- **Treino nunca se perde** — o andamento é salvo a cada série marcada; se o app for fechado no meio, ele oferece continuar de onde parou.
- **Histórico** — cada sessão com duração, séries concluídas, total de repetições/tempo e o detalhe de cada série.

## Destaques técnicos

- **100% Kotlin e SDK do Android, sem nenhuma biblioteca externa** — nem AndroidX. O APK fica pequeno e não há dependência para atualizar.
- **Interface montada em código**, sem XML de layout, com um pequeno conjunto de componentes próprios (cards, botões, cabeçalho com ondas desenhadas em `Canvas`).
- **Lista com arrastar e deslizar feita do zero** ([`CardList.kt`](app/src/main/java/com/aquafit/app/ui/CardList.kt)) tratando `MotionEvent` direto: reordenação animada, ações reveladas por swipe e auto-scroll.
- **SQLite com `SQLiteOpenHelper`**, chaves estrangeiras com `ON DELETE CASCADE`, índices e migração de schema versionada.
- **Recuperação de estado** do treino em andamento persistida em JSON, independente do ciclo de vida da Activity (sobrevive à morte do processo).
- **CI/CD com GitHub Actions** — cada push na `main` compila e publica o APK em [Releases](https://github.com/MatheusTogni/AquaFit/releases/latest).

## Estrutura

```
app/src/main/java/com/aquafit/app/
├── MainActivity.kt            lista de treinos
├── WorkoutEditActivity.kt     exercícios de um treino
├── ExerciseEditDialog.kt      formulário do exercício
├── WorkoutRunActivity.kt      execução do treino
├── HistoryActivity.kt         histórico de sessões
├── SessionDetailActivity.kt   detalhe de uma sessão
├── data/                      SQLite (Db, Repository), modelos e rascunho do treino
└── ui/                        tema, componentes, formatação e a lista com gestos
```

## Instalação

Baixe o `AquaFit.apk` em **[Releases → latest](https://github.com/MatheusTogni/AquaFit/releases/latest)** pelo celular e instale (o Android pede para permitir apps de fontes desconhecidas). Versões novas instalam por cima sem perder os dados.

## Compilando

Requer JDK 17 e Android SDK 35.

```bash
gradle assembleDebug
# APK em app/build/outputs/apk/debug/app-debug.apk
```

Ou faça um fork e deixe o GitHub Actions compilar ([`build.yml`](.github/workflows/build.yml)).

---

Fonte [Nunito](https://github.com/googlefonts/nunito) sob a [SIL Open Font License](licenses/Nunito-OFL.txt).
