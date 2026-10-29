package com.aquafit.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import com.aquafit.app.MainActivity.Companion.EXTRA_WORKOUT_ID
import com.aquafit.app.data.Exercise
import com.aquafit.app.data.Repository
import com.aquafit.app.data.Workout
import com.aquafit.app.ui.BtnStyle
import com.aquafit.app.ui.CardList
import com.aquafit.app.ui.Fmt
import com.aquafit.app.ui.INPUT_MULTILINE
import com.aquafit.app.ui.OceanActivity
import com.aquafit.app.ui.Palette
import com.aquafit.app.ui.SwipeAction
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

class WorkoutEditActivity : OceanActivity() {

    private val repo by lazy { Repository.get(this) }
    private var workoutId = 0L
    private lateinit var workout: Workout
    private var exercises: List<Exercise> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, 0L)
        val w = repo.workout(workoutId)
        if (w == null) {
            finish()
            return
        }
        workout = w
        setupScreen(w.name, "Editar treino")
        addHeaderAction("✎") { promptRename() }
        addHeaderIcon(R.drawable.ic_trash, "Excluir treino") {
            confirm("Excluir treino", "Excluir \"${workout.name}\"? O histórico continua salvo.", "Excluir") {
                repo.deleteWorkout(workoutId)
                finish()
            }
        }
        addBottomButton("+ Exercício", BtnStyle.SECONDARY) { showExerciseDialog(workoutId, null) { render() } }
        addBottomButton("▶ Iniciar", BtnStyle.PRIMARY) {
            if (exercises.isEmpty()) {
                Toast.makeText(this, "Adicione pelo menos um exercício 🌊", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, WorkoutRunActivity::class.java).putExtra(EXTRA_WORKOUT_ID, workoutId))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::workout.isInitialized) render()
    }

    private fun render() {
        workout = repo.workout(workoutId) ?: return finish()
        exercises = repo.exercises(workoutId)
        setHeader(workout.name, if (workout.description.isBlank()) "Editar treino" else workout.description)
        content.removeAllViews()

        if (exercises.isEmpty()) {
            emptyState("🐠", "Nenhum exercício ainda", "Toque em \"+ Exercício\" para adicionar flexões, barras, dips…")
            return
        }
        val list = CardList(scroll) { from, to ->
            repo.reorderExercises(exercises.toMutableList().apply { add(to, removeAt(from)) })
            render()
        }
        exercises.forEachIndexed { index, e ->
            content.addView(
                list.add(
                    exerciseCard(index, e),
                    onTap = { editExercise(e) },
                    swipeLeft = SwipeAction(Palette.DANGER, iconRes = R.drawable.ic_trash) { reset ->
                        confirm("Remover exercício", "Remover \"${e.name}\" deste treino?", "Remover", onDismiss = reset) {
                            repo.deleteExercise(e.id)
                            render()
                        }
                    },
                    swipeRight = SwipeAction(Palette.TURQUOISE, icon = "✎") { reset ->
                        reset()
                        editExercise(e)
                    }
                )
            )
        }
    }

    private fun exerciseCard(index: Int, e: Exercise) = card().apply {
        val top = horizontal().apply {
            addView(
                text("${index + 1}", 15f, Palette.WHITE, bold = true).apply {
                    gravity = Gravity.CENTER
                    background = rounded(Palette.TURQUOISE, dpf(16f))
                },
                lp(dp(32), dp(32)).margins(r = dp(12))
            )
            val info = vertical().apply {
                addView(text(e.name, 17f, Palette.DEEP, bold = true))
                addView(text(Fmt.target(e), 14f, Palette.OCEAN), lp().margins(t = dp(2)))
            }
            addView(info, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(top)
        if (e.notes.isNotBlank()) {
            addView(text("📝 ${e.notes}", 13f, Palette.MUTED), lp().margins(t = dp(8)))
        }
    }

    private fun editExercise(e: Exercise) = showExerciseDialog(workoutId, e) { render() }

    private fun promptRename() {
        val name = input("Nome do treino", workout.name)
        val desc = input("Opcional", workout.description, INPUT_MULTILINE)
        val form = vertical(dp(20)).apply {
            addView(field("Nome do treino", name))
            addView(field("Descrição", desc))
        }
        val dialog = dialogBuilder()
            .setTitle("Editar treino")
            .setView(form)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = name.text.toString().trim()
                if (n.isEmpty()) {
                    name.error = "Informe um nome"
                    return@setOnClickListener
                }
                repo.saveWorkout(workout.copy(name = n, description = desc.text.toString().trim()))
                dialog.dismiss()
                render()
            }
        }
        dialog.show()
    }
}
