package com.aquafit.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.aquafit.app.data.Repository
import com.aquafit.app.data.RunDraft
import com.aquafit.app.data.Workout
import com.aquafit.app.ui.BtnStyle
import com.aquafit.app.ui.CARD_RADIUS
import com.aquafit.app.ui.CardList
import com.aquafit.app.ui.Fmt
import com.aquafit.app.ui.INPUT_MULTILINE
import com.aquafit.app.ui.OceanActivity
import com.aquafit.app.ui.Palette
import com.aquafit.app.ui.SwipeAction
import com.aquafit.app.ui.badge
import com.aquafit.app.ui.button
import com.aquafit.app.ui.card
import com.aquafit.app.ui.confirm
import com.aquafit.app.ui.dialogBuilder
import com.aquafit.app.ui.dp
import com.aquafit.app.ui.dpf
import com.aquafit.app.ui.field
import com.aquafit.app.ui.horizontal
import com.aquafit.app.ui.input
import com.aquafit.app.ui.lp
import com.aquafit.app.ui.margins
import com.aquafit.app.ui.rounded
import com.aquafit.app.ui.text
import com.aquafit.app.ui.vertical
import com.aquafit.app.update.Updater

class MainActivity : OceanActivity() {

    private val repo by lazy { Repository.get(this) }
    private var resumeDialog: AlertDialog? = null

    private var updateCardView: View? = null
    private var updateDismissed = false
    private var installAfterPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen("Aqua Fit 🌊", "Seus treinos de calistenia", showBack = false)
        setHeaderNote("v${appVersion()}")
        addHeaderAction("🐚 Histórico") { startActivity(Intent(this, HistoryActivity::class.java)) }
        addFab("+") { promptNewWorkout() }
        Updater.check()
    }

    override fun onStart() {
        super.onStart()
        Updater.listener = { refreshUpdateCard() }
        Updater.onConfirm = { startActivity(it) }
    }

    override fun onStop() {
        Updater.listener = null
        Updater.onConfirm = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (installAfterPermission && packageManager.canRequestPackageInstalls()) {
            installAfterPermission = false
            Updater.download(this)
        }
        render()
        offerResume()
    }

    /** Se o app foi fechado no meio de um treino, oferece continuar de onde parou. */
    private fun offerResume() {
        if (resumeDialog?.isShowing == true) return
        val draft = RunDraft.load(this) ?: return
        if (repo.workout(draft.workoutId) == null) {
            RunDraft.clear(this)
            return
        }
        resumeDialog = dialogBuilder()
            .setTitle("Treino em andamento 🏄")
            .setMessage("Você tem um treino em andamento: ${draft.workoutName}. Continuar?")
            .setCancelable(false)
            .setPositiveButton("Continuar") { _, _ ->
                startActivity(Intent(this, WorkoutRunActivity::class.java).putExtra(EXTRA_WORKOUT_ID, draft.workoutId))
            }
            .setNegativeButton("Descartar") { _, _ -> RunDraft.clear(this) }
            .show()
    }

    private fun render() {
        content.removeAllViews()
        updateCardView = updateCard()?.also { content.addView(it) }
        val workouts = repo.workouts()
        if (workouts.isEmpty()) {
            emptyState("🏝️", "Mar calmo por aqui…", "Toque no + para criar seu primeiro treino.")
        } else {
            val list = CardList(scroll) { from, to ->
                repo.reorderWorkouts(workouts.toMutableList().apply { add(to, removeAt(from)) })
                render()
            }
            workouts.forEach { w ->
                content.addView(
                    list.add(
                        workoutCard(w),
                        onTap = { openEditor(w.id) },
                        swipeLeft = SwipeAction(Palette.DANGER, iconRes = R.drawable.ic_trash) { reset -> confirmDelete(w, reset) }
                    )
                )
            }
        }
    }

    /** Card no topo quando há versão nova no GitHub. Nada é baixado até o usuário tocar em "Atualizar". */
    private fun updateCard(): View? {
        val release = Updater.available ?: return null
        if (updateDismissed) return null

        val box = card()
        box.background = rounded(Palette.WHITE, dpf(CARD_RADIUS), Palette.TURQUOISE, dp(2))
        box.layoutParams = lp().margins(t = dp(4), b = dp(7))

        val head = horizontal()
        head.addView(text("🌊", 26f).apply {
            gravity = Gravity.CENTER
            background = rounded(Palette.SKY, dpf(14f))
        }, lp(dp(44), dp(44)))
        val texts = vertical()
        texts.addView(text("Nova versão disponível", 17f, Palette.DEEP, bold = true))
        texts.addView(text("Atualizar para a versão ${release.versionName}?", 14f, Palette.MUTED))
        head.addView(texts, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(12)))
        box.addView(head)

        if (release.notes.isNotEmpty()) {
            val notes = text(release.notes, 13f, Palette.MUTED)
            notes.maxLines = 2
            notes.ellipsize = TextUtils.TruncateAt.END
            box.addView(notes, lp().margins(t = dp(10)))
        }

        val error = Updater.error
        if (Updater.phase == Updater.Phase.FAILED && error != null) {
            box.addView(text(error, 13f, Palette.DANGER, bold = true), lp().margins(t = dp(10)))
        }

        val actions = horizontal()
        actions.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        when (Updater.phase) {
            Updater.Phase.DOWNLOADING ->
                actions.addView(text("Baixando… ${Updater.progress}%", 14f, Palette.OCEAN, bold = true), lp(wrap).margins(r = dp(6)))
            Updater.Phase.INSTALLING -> {
                actions.addView(text("Aguardando o instalador…", 14f, Palette.OCEAN, bold = true), lp(0, wrap, 1f))
                actions.addView(button("Tentar de novo", BtnStyle.GHOST) { startUpdate() }, lp(wrap))
            }
            else -> {
                actions.addView(button("Agora não", BtnStyle.GHOST) {
                    updateDismissed = true
                    refreshUpdateCard()
                }, lp(wrap))
                val label = if (Updater.phase == Updater.Phase.FAILED) "Tentar de novo" else "Atualizar"
                actions.addView(button(label, BtnStyle.PRIMARY) { startUpdate() }, lp(wrap).margins(l = dp(8)))
            }
        }
        box.addView(actions, lp().margins(t = dp(12)))
        return box
    }

    /** Troca só o card de atualização, sem redesenhar a lista (progresso do download etc.). */
    private fun refreshUpdateCard() {
        val fresh = updateCard()
        val old = updateCardView
        if (old != null && old.parent === content) {
            val index = content.indexOfChild(old)
            content.removeViewAt(index)
            if (fresh != null) content.addView(fresh, index)
        } else if (fresh != null) {
            content.addView(fresh, 0)
        }
        updateCardView = fresh
    }

    private fun startUpdate() {
        if (!packageManager.canRequestPackageInstalls()) {
            confirm(
                "Permitir atualizações",
                "Para instalar a versão nova, o Android precisa que o Aqua Fit tenha permissão para instalar apps. " +
                    "Na próxima tela, ative \"Permitir desta fonte\" e volte para o Aqua Fit.",
                "Abrir ajustes"
            ) {
                installAfterPermission = true
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            }
            return
        }
        Updater.download(this)
    }

    private fun appVersion(): String =
        try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }

    private fun workoutCard(w: Workout) = card().apply {
        addView(text(w.name, 19f, Palette.DEEP, bold = true))
        if (w.description.isNotBlank()) {
            addView(text(w.description, 14f, Palette.MUTED), lp().margins(t = dp(2)))
        }
        val badges = horizontal().apply {
            val count = if (w.exerciseCount == 1) "1 exercício" else "${w.exerciseCount} exercícios"
            addView(badge("🏊 $count"))
            val last = repo.lastSessionAt(w.id)
            addView(
                badge(if (last != null) "☀️ último: ${Fmt.shortDate(last)}" else "☀️ nunca feito", Palette.SAND, 0xFFB35C00.toInt()),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT).margins(l = dp(6))
            )
        }
        addView(badges, lp().margins(t = dp(10)))
    }

    private fun confirmDelete(w: Workout, reset: () -> Unit) {
        confirm(
            "Excluir treino",
            "Excluir \"${w.name}\" e seus exercícios? O histórico continua salvo.",
            "Excluir",
            onDismiss = reset
        ) {
            repo.deleteWorkout(w.id)
            render()
        }
    }

    private fun openEditor(id: Long) {
        startActivity(Intent(this, WorkoutEditActivity::class.java).putExtra(EXTRA_WORKOUT_ID, id))
    }

    private fun promptNewWorkout() {
        val name = input("Ex.: Treino A – Empurrar")
        val desc = input("Opcional", type = INPUT_MULTILINE)
        val form = vertical(dp(20)).apply {
            addView(field("Nome do treino", name))
            addView(field("Descrição", desc))
        }
        val dialog = dialogBuilder()
            .setTitle("Novo treino 🌊")
            .setView(form)
            .setPositiveButton("Criar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = name.text.toString().trim()
                if (n.isEmpty()) {
                    name.error = "Informe um nome"
                    return@setOnClickListener
                }
                val id = repo.saveWorkout(Workout(name = n, description = desc.text.toString().trim()))
                dialog.dismiss()
                openEditor(id)
            }
        }
        dialog.show()
    }

    companion object {
        const val EXTRA_WORKOUT_ID = "workout_id"
        const val EXTRA_SESSION_ID = "session_id"
    }
}
