package com.aquafit.app

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import com.aquafit.app.MainActivity.Companion.EXTRA_SESSION_ID
import com.aquafit.app.data.Repository
import com.aquafit.app.data.Session
import com.aquafit.app.ui.Fmt
import com.aquafit.app.ui.OceanActivity
import com.aquafit.app.ui.Palette
import com.aquafit.app.ui.badge
import com.aquafit.app.ui.card
import com.aquafit.app.ui.confirm
import com.aquafit.app.ui.dp
import com.aquafit.app.ui.horizontal
import com.aquafit.app.ui.lp
import com.aquafit.app.ui.margins
import com.aquafit.app.ui.text

class HistoryActivity : OceanActivity() {

    private val repo by lazy { Repository.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen("Histórico 🐚", "Seus treinos realizados")
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        content.removeAllViews()
        val sessions = repo.sessions()
        if (sessions.isEmpty()) {
            emptyState("🌅", "Nenhum treino registrado", "Inicie um treino e finalize para vê-lo aqui.")
            return
        }
        setHeader("Histórico 🐚", if (sessions.size == 1) "1 treino realizado" else "${sessions.size} treinos realizados")
        sessions.forEach { content.addView(sessionCard(it)) }
    }

    private fun sessionCard(s: Session) = card().apply {
        addView(text(s.workoutName, 18f, Palette.DEEP, bold = true))
        addView(text(Fmt.dateTime(s.startedAt), 13f, Palette.MUTED), lp().margins(t = dp(2)))
        addView(horizontal().apply {
            addView(badge("⏱ ${Fmt.elapsed(s.finishedAt - s.startedAt)}"))
            addView(
                badge("🌊 ${s.doneSets}/${s.totalSets} séries", Palette.SAND, 0xFFB35C00.toInt()),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT).margins(l = dp(6))
            )
        }, lp().margins(t = dp(10)))
        setOnClickListener {
            startActivity(Intent(this@HistoryActivity, SessionDetailActivity::class.java).putExtra(EXTRA_SESSION_ID, s.id))
        }
        setOnLongClickListener {
            confirm("Excluir registro", "Excluir este treino do histórico?", "Excluir") {
                repo.deleteSession(s.id)
                render()
            }
            true
        }
    }
}
