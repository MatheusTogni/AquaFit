package com.aquafit.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.aquafit.app.MainActivity.Companion.EXTRA_WORKOUT_ID
import com.aquafit.app.data.Exercise
import com.aquafit.app.data.ExerciseMode
import com.aquafit.app.data.Repository
import com.aquafit.app.data.RunDraft
import com.aquafit.app.data.Session
import com.aquafit.app.data.SessionSet
import com.aquafit.app.data.Workout
import com.aquafit.app.ui.BtnStyle
import com.aquafit.app.ui.Fmt
import com.aquafit.app.ui.INPUT_DECIMAL_SIGNED
import com.aquafit.app.ui.INPUT_INT
import com.aquafit.app.ui.OceanActivity
import com.aquafit.app.ui.Palette
import com.aquafit.app.ui.card
import com.aquafit.app.ui.confirm
import com.aquafit.app.ui.dp
import com.aquafit.app.ui.dpf
import com.aquafit.app.ui.horizontal
import com.aquafit.app.ui.input
import com.aquafit.app.ui.lp
import com.aquafit.app.ui.margins
import com.aquafit.app.ui.rounded
import com.aquafit.app.ui.text

class WorkoutRunActivity : OceanActivity() {

    /** Uma linha de série na tela, com os campos que o usuário pode ajustar. */
    private class SetRow(
        val exercise: Exercise,
        val number: Int,
        val row: LinearLayout,
        val amount: EditText,
        val weight: EditText,
        val check: CheckBox
    )

    private val repo by lazy { Repository.get(this) }
    private lateinit var workout: Workout
    private val rows = mutableListOf<SetRow>()
    private var startedAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateHeader()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, 0L)
        val w = repo.workout(workoutId)
        val exercises = w?.let { repo.exercises(it.id) }.orEmpty()
        if (w == null || exercises.isEmpty()) {
            if (RunDraft.load(this)?.workoutId == workoutId) RunDraft.clear(this)
            finish()
            return
        }
        workout = w
        // Retoma o treino em andamento se o app foi fechado no meio dele.
        val draft = RunDraft.load(this)?.takeIf { it.workoutId == w.id }
        startedAt = draft?.startedAt ?: System.currentTimeMillis()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupScreen("🏄 ${w.name}", "")
        exercises.forEach { content.addView(exerciseCard(it, draft)) }
        saveDraft()

        addBottomButton("Cancelar", BtnStyle.OUTLINE) { onBackPressed() }
        addBottomButton("🏁 Finalizar", BtnStyle.PRIMARY) { finishWorkout() }
    }

    override fun onResume() {
        super.onResume()
        if (::workout.isInitialized) handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun updateHeader() {
        val done = rows.count { it.check.isChecked }
        setHeader("🏄 ${workout.name}", "⏱ ${Fmt.elapsed(System.currentTimeMillis() - startedAt)}  ·  $done/${rows.size} séries")
    }

    private fun saveDraft() {
        RunDraft.save(
            this,
            RunDraft(
                workoutId = workout.id,
                workoutName = workout.name,
                startedAt = startedAt,
                sets = rows.map {
                    RunDraft.SetState(it.exercise.id, it.number, it.amount.text.toString(), it.weight.text.toString(), it.check.isChecked)
                }
            )
        )
    }

    private fun exerciseCard(e: Exercise, draft: RunDraft?) = card().apply {
        addView(text(e.name, 18f, Palette.DEEP, bold = true))
        addView(text("Meta: ${Fmt.target(e)}", 13f, Palette.OCEAN), lp().margins(t = dp(2)))
        if (e.notes.isNotBlank()) addView(text("📝 ${e.notes}", 13f, Palette.MUTED), lp().margins(t = dp(2)))

        val unit = if (e.mode == ExerciseMode.REPS) "reps" else "seg"
        addView(horizontal().apply {
            addView(text("Série", 12f, Palette.MUTED, bold = true), lp(dp(56)))
            addView(text(unit, 12f, Palette.MUTED, bold = true), lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(4)))
            addView(text("kg", 12f, Palette.MUTED, bold = true), lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(8)))
            addView(text("Feito", 12f, Palette.MUTED, bold = true).apply { gravity = Gravity.CENTER }, lp(dp(52)))
        }, lp().margins(t = dp(12), l = dp(8), r = dp(4)))

        for (n in 1..e.sets) addView(setRow(e, n, draft?.find(e.id, n)))
    }

    private fun setRow(e: Exercise, n: Int, saved: RunDraft.SetState?): LinearLayout {
        val done = saved?.done == true
        val row = horizontal().apply {
            setPadding(dp(8), dp(4), dp(4), dp(4))
            background = rounded(if (done) Palette.DONE_BG else 0x00000000, dpf(14f))
            layoutParams = lp().margins(t = dp(6))
        }
        val amountValue = if (e.mode == ExerciseMode.REPS) e.reps else e.seconds
        val amount = input("", saved?.amount ?: amountValue.toString(), INPUT_INT).apply { gravity = Gravity.CENTER }
        val weight = input("0", saved?.weight ?: if (e.weightKg != 0.0) Fmt.num(e.weightKg) else "", INPUT_DECIMAL_SIGNED)
            .apply { gravity = Gravity.CENTER }
        amount.addTextChangedListener(onEdit)
        weight.addTextChangedListener(onEdit)
        val check = CheckBox(this).apply {
            buttonTintList = ColorStateList.valueOf(Palette.TURQUOISE)
            scaleX = 1.3f
            scaleY = 1.3f
            isChecked = done
            setOnCheckedChangeListener { _, checked ->
                row.background = rounded(if (checked) Palette.DONE_BG else 0x00000000, dpf(14f))
                updateHeader()
                saveDraft()
            }
        }
        row.addView(text("$n", 16f, Palette.DEEP, bold = true), lp(dp(56)))
        row.addView(amount, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(4)))
        row.addView(weight, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(8)))
        row.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(check)
        }, lp(dp(52)))
        rows += SetRow(e, n, row, amount, weight, check)
        return row
    }

    private val onEdit = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) = saveDraft()
    }

    private fun finishWorkout() {
        val done = rows.count { it.check.isChecked }
        if (done == 0) {
            confirm("Nenhuma série marcada", "Salvar a sessão mesmo assim?", "Salvar") { save() }
        } else {
            save()
        }
    }

    private fun save() {
        val sets = rows.map { r ->
            val value = Fmt.parseInt(r.amount.text.toString()) ?: 0
            val isReps = r.exercise.mode == ExerciseMode.REPS
            SessionSet(
                exerciseName = r.exercise.name,
                setNumber = r.number,
                mode = r.exercise.mode,
                reps = if (isReps) value else 0,
                seconds = if (isReps) 0 else value,
                weightKg = Fmt.parseDouble(r.weight.text.toString()) ?: 0.0,
                done = r.check.isChecked
            )
        }
        repo.saveSession(
            Session(
                workoutId = workout.id,
                workoutName = workout.name,
                startedAt = startedAt,
                finishedAt = System.currentTimeMillis()
            ),
            sets
        )
        RunDraft.clear(this)
        Toast.makeText(this, "Treino salvo no histórico 🌊", Toast.LENGTH_LONG).show()
        finish()
    }

    @Deprecated("Usado para confirmar antes de sair do treino")
    override fun onBackPressed() {
        if (!::workout.isInitialized) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
            return
        }
        confirm("Sair do treino?", "As séries marcadas não serão salvas.", "Sair") {
            RunDraft.clear(this)
            finish()
        }
    }
}
