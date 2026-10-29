package com.aquafit.app

import android.os.Bundle
import android.view.ViewGroup
import com.aquafit.app.MainActivity.Companion.EXTRA_SESSION_ID
import com.aquafit.app.data.ExerciseMode
import com.aquafit.app.data.Repository
import com.aquafit.app.data.SessionSet
import com.aquafit.app.ui.Fmt
import com.aquafit.app.ui.OceanActivity
import com.aquafit.app.ui.Palette
import com.aquafit.app.ui.card
import com.aquafit.app.ui.confirm
import com.aquafit.app.ui.dp
import com.aquafit.app.ui.horizontal
import com.aquafit.app.ui.lp
import com.aquafit.app.ui.margins
import com.aquafit.app.ui.text
import com.aquafit.app.ui.vertical

class SessionDetailActivity : OceanActivity() {

    private val repo by lazy { Repository.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
        val session = repo.session(id)
        if (session == null) {
            finish()
            return
        }
        setupScreen(session.workoutName, Fmt.dateTime(session.startedAt))
        addHeaderIcon(R.drawable.ic_trash, "Excluir registro") {
            confirm("Excluir registro", "Excluir este treino do histórico?", "Excluir") {
                repo.deleteSession(id)
                finish()
            }
        }

        val sets = repo.sessionSets(id)
        val done = sets.filter { it.done }
        val totalReps = done.filter { it.mode == ExerciseMode.REPS }.sumOf { it.reps }
        val totalSeconds = done.filter { it.mode == ExerciseMode.TIME }.sumOf { it.seconds }

        content.addView(card().apply {
            addView(horizontal().apply {
                addView(stat("⏱", Fmt.elapsed(session.finishedAt - session.startedAt), "duração"), weighted())
                addView(stat("🌊", "${done.size}/${sets.size}", "séries"), weighted())
                addView(
                    if (totalSeconds > 0 && totalReps == 0) stat("🏖️", Fmt.duration(totalSeconds), "em isometria")
                    else stat("🔁", "$totalReps", "repetições"),
                    weighted()
                )
            })
            if (totalSeconds > 0 && totalReps > 0) {
                addView(text("+ ${Fmt.duration(totalSeconds)} em exercícios por tempo", 13f, Palette.MUTED), lp().margins(t = dp(8)))
            }
            if (session.notes.isNotBlank()) {
                addView(text("📝 ${session.notes}", 14f, Palette.MUTED), lp().margins(t = dp(10)))
            }
        })

        sectionTitle("SÉRIES")
        sets.groupBy { it.exerciseName }.forEach { (name, list) ->
            content.addView(card().apply {
                addView(text(name, 17f, Palette.DEEP, bold = true))
                list.forEach { addView(setLine(it), lp().margins(t = dp(6))) }
            })
        }
    }

    private fun weighted() = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    private fun stat(emoji: String, value: String, label: String) = vertical().apply {
        gravity = android.view.Gravity.CENTER_HORIZONTAL
        addView(text(emoji, 20f).apply { gravity = android.view.Gravity.CENTER })
        addView(text(value, 18f, Palette.DEEP, bold = true).apply { gravity = android.view.Gravity.CENTER })
        addView(text(label, 12f, Palette.MUTED).apply { gravity = android.view.Gravity.CENTER })
    }

    private fun setLine(s: SessionSet) = horizontal().apply {
        val color = if (s.done) Palette.OCEAN else Palette.MUTED
        addView(text(if (s.done) "✅" else "⬜", 15f), lp(dp(32)))
        addView(
            text("Série ${s.setNumber}: ${Fmt.amount(s.mode, s.reps, s.seconds)} · ${Fmt.kg(s.weightKg)}", 15f, color),
            lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
    }
}
