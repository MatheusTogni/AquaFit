package com.aquafit.app

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import com.aquafit.app.data.Exercise
import com.aquafit.app.data.ExerciseMode
import com.aquafit.app.data.Repository
import com.aquafit.app.ui.Fmt
import com.aquafit.app.ui.INPUT_DECIMAL_SIGNED
import com.aquafit.app.ui.INPUT_INT
import com.aquafit.app.ui.INPUT_MULTILINE
import com.aquafit.app.ui.Palette
import com.aquafit.app.ui.dialogBuilder
import com.aquafit.app.ui.dp
import com.aquafit.app.ui.dpf
import com.aquafit.app.ui.field
import com.aquafit.app.ui.horizontal
import com.aquafit.app.ui.input
import com.aquafit.app.ui.lp
import com.aquafit.app.ui.margins
import com.aquafit.app.ui.ripple
import com.aquafit.app.ui.rounded
import com.aquafit.app.ui.text
import com.aquafit.app.ui.vertical

/** Formulário de exercício (novo ou edição) num diálogo. */
fun Activity.showExerciseDialog(workoutId: Long, existing: Exercise?, onSaved: () -> Unit) {
    val e = existing ?: Exercise(workoutId = workoutId, name = "")
    var mode = e.mode

    val name = input("Ex.: Flexão, Barra fixa, Prancha", e.name)
    val sets = input("3", if (existing != null) e.sets.toString() else "", INPUT_INT)
    val reps = input("10", if (existing != null) e.reps.toString() else "", INPUT_INT)
    val seconds = input("30", if (existing != null) e.seconds.toString() else "", INPUT_INT)
    val weight = input("0 = peso corporal", if (e.weightKg != 0.0) Fmt.num(e.weightKg) else "", INPUT_DECIMAL_SIGNED)
    val notes = input("Opcional (pegada, progressão…)", e.notes, INPUT_MULTILINE)

    val repsField = field("Repetições por série", reps)
    val secondsField = field("Tempo por série (segundos)", seconds)

    lateinit var repsBtn: TextView
    lateinit var timeBtn: TextView
    fun segment(label: String, onClick: () -> Unit) = text(label, 14f, Palette.OCEAN, bold = true).apply {
        gravity = android.view.Gravity.CENTER
        setPadding(dp(8), dp(10), dp(8), dp(10))
        setOnClickListener { onClick() }
    }
    fun applyMode() {
        val active = rounded(Palette.TURQUOISE, dpf(20f))
        val idle = rounded(Palette.WHITE, dpf(20f), Palette.TURQUOISE, dp(2))
        repsBtn.background = ripple(if (mode == ExerciseMode.REPS) active else idle)
        repsBtn.setTextColor(if (mode == ExerciseMode.REPS) Palette.WHITE else Palette.OCEAN)
        timeBtn.background = ripple(if (mode == ExerciseMode.TIME) active else idle)
        timeBtn.setTextColor(if (mode == ExerciseMode.TIME) Palette.WHITE else Palette.OCEAN)
        repsField.visibility = if (mode == ExerciseMode.REPS) View.VISIBLE else View.GONE
        secondsField.visibility = if (mode == ExerciseMode.TIME) View.VISIBLE else View.GONE
    }
    repsBtn = segment("🔁 Repetições") { mode = ExerciseMode.REPS; applyMode() }
    timeBtn = segment("⏱️ Tempo") { mode = ExerciseMode.TIME; applyMode() }

    val modeRow = horizontal().apply {
        addView(repsBtn, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(r = dp(4)))
        addView(timeBtn, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(4)))
    }

    val form = vertical(dp(20)).apply {
        addView(field("Exercício", name))
        addView(field("Tipo", modeRow))
        addView(field("Séries", sets))
        addView(repsField)
        addView(secondsField)
        addView(field("Carga (kg) — negativo = assistência (elástico)", weight))
        addView(field("Observações", notes))
    }
    applyMode()

    val dialog = dialogBuilder()
        .setTitle(if (existing == null) "Novo exercício 🏊" else "Editar exercício")
        .setView(ScrollView(this).apply { addView(form) })
        .setPositiveButton("Salvar", null)
        .setNegativeButton("Cancelar", null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = name.text.toString().trim()
            if (n.isEmpty()) {
                name.error = "Informe o exercício"
                return@setOnClickListener
            }
            val s = Fmt.parseInt(sets.text.toString()) ?: 3
            if (s < 1) {
                sets.error = "Mínimo 1 série"
                return@setOnClickListener
            }
            val weightText = weight.text.toString()
            val w = if (weightText.isBlank()) 0.0 else Fmt.parseDouble(weightText)
            if (w == null) {
                weight.error = "Valor inválido"
                return@setOnClickListener
            }
            Repository.get(this).saveExercise(
                e.copy(
                    name = n,
                    sets = s,
                    mode = mode,
                    reps = Fmt.parseInt(reps.text.toString()) ?: 10,
                    seconds = Fmt.parseInt(seconds.text.toString()) ?: 30,
                    weightKg = w,
                    notes = notes.text.toString().trim()
                )
            )
            dialog.dismiss()
            onSaved()
        }
    }
    dialog.show()
}
