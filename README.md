# 🌊 Aqua Fit

App Android para guardar seus treinos de calistenia: crie treinos, adicione exercícios (séries, repetições ou tempo, carga em kg), execute o treino marcando as séries e consulte o histórico. Tudo fica salvo no celular (SQLite).

## Como gerar e instalar o APK

1. **Suba para o GitHub na branch `main`:**
   ```bash
   git init
   git add .
   git commit -m "Aqua Fit"
   git branch -M main
   git remote add origin https://github.com/SEU_USUARIO/aqua-fit.git
   git push -u origin main
   ```
2. **Espere a aba _Actions_ terminar** (o job "Build APK" fica verde em poucos minutos).
3. **Baixe o APK pelo celular:** no repositório, abra **Releases → latest** e toque em `AquaFit.apk`. Permita a instalação de fontes desconhecidas se o Android pedir.

Cada novo push na `main` gera uma nova release `latest`. Como o APK é sempre assinado com o mesmo `app/debug.keystore`, a versão nova instala por cima da anterior sem perder seus dados.
