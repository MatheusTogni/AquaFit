package com.aquafit.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import com.aquafit.app.data.Repository
import com.aquafit.app.data.RunDraft
import com.aquafit.app.data.Workout
import com.aquafit.app.ui.CardList
import com.aquafit.app.ui.Fmt
import com.aquafit.app.ui.INPUT_MULTILINE
import com.aquafit.app.ui.OceanActivity
import com.aquafit.app.ui.Palette
import com.aquafit.app.ui.SwipeAction
import com.aquafit.app.ui.badge
import com.aquafit.app.ui.card
import com.aquafit.app.ui.confirm
import com.aquafit.app.ui.dialogBuilder
import com.aquafit.app.ui.dp
import com.aquafit.app.ui.field
import com.aquafit.app.ui.horizontal
import com.aquafit.app.ui.input
import com.aquafit.app.ui.lp
import com.aquafit.app.ui.margins
import com.aquafit.app.ui.text
import com.aquafit.app.ui.vertical

class MainActivity : OceanActivity() {

    private val repo by lazy { Repository.get(this) }
    private var resumeDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen("Aqua Fit 🌊", "Seus treinos de calistenia", showBack = false)
        setHeaderNote("v${appVersion()}")
        addHeaderAction("🐚 Histórico") { startActivity(Intent(this, HistoryActivity::class.java)) }
        addFab("+") { promptNewWorkout() }
    }

    override fun onResume() {
        super.onResume()
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
        val workouts = repo.workouts()
        if (workouts.isEmpty()) {
            emptyState("🏝️", "Mar calmo por aqui…", "Toque no + para criar seu primeiro treino.")
        } else {
            sectionTitle("MEUS TREINOS")
            hint("Toque para abrir · segure e arraste para reordenar · deslize ← para excluir")
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
