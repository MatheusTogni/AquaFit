package com.aquafit.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import com.aquafit.app.data.Repository
import com.aquafit.app.data.Workout
import com.aquafit.app.ui.BtnStyle
import com.aquafit.app.ui.DragReorder
import com.aquafit.app.ui.Fmt
import com.aquafit.app.ui.INPUT_MULTILINE
import com.aquafit.app.ui.OceanActivity
import com.aquafit.app.ui.Palette
import com.aquafit.app.ui.badge
import com.aquafit.app.ui.button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen("Aqua Fit 🌊", "Seus treinos de calistenia", showBack = false)
        addHeaderAction("🐚 Histórico") { startActivity(Intent(this, HistoryActivity::class.java)) }
        addFab("+") { promptNewWorkout() }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        content.removeAllViews()
        val workouts = repo.workouts()
        if (workouts.isEmpty()) {
            emptyState("🏝️", "Mar calmo por aqui…", "Toque no + para criar seu primeiro treino.")
        } else {
            sectionTitle("MEUS TREINOS")
            val drag = DragReorder(scroll) { from, to ->
                repo.reorderWorkouts(workouts.toMutableList().apply { add(to, removeAt(from)) })
                render()
            }
            workouts.forEach { content.addView(workoutCard(it, drag)) }
        }
        content.addView(
            text("v${appVersion()}", 11f, 0x885C7C99.toInt()).apply { gravity = Gravity.CENTER },
            lp().margins(t = dp(24))
        )
    }

    private fun appVersion(): String =
        try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }

    private fun workoutCard(w: Workout, drag: DragReorder) = card().apply {
        val handle = drag.handle()
        val titleRow = horizontal().apply {
            addView(text(w.name, 19f, Palette.DEEP, bold = true), lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(handle, lp(dp(40), dp(36)).margins(l = dp(8)))
        }
        addView(titleRow)
        drag.add(this, handle)
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

        val actions = horizontal().apply {
            addView(
                button("▶ Iniciar", BtnStyle.PRIMARY) { startWorkout(w) },
                lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(r = dp(6))
            )
            addView(
                button("Editar", BtnStyle.OUTLINE) { openEditor(w.id) },
                lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(6))
            )
        }
        addView(actions, lp().margins(t = dp(12)))

        setOnClickListener { openEditor(w.id) }
        setOnLongClickListener {
            confirm("Excluir treino", "Excluir \"${w.name}\" e seus exercícios? O histórico continua salvo.", "Excluir") {
                repo.deleteWorkout(w.id)
                render()
            }
            true
        }
    }

    private fun startWorkout(w: Workout) {
        if (w.exerciseCount == 0) {
            Toast.makeText(this, "Adicione exercícios antes de iniciar 🌊", Toast.LENGTH_SHORT).show()
            openEditor(w.id)
            return
        }
        startActivity(Intent(this, WorkoutRunActivity::class.java).putExtra(EXTRA_WORKOUT_ID, w.id))
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
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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
