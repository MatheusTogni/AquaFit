package com.aquafit.app.ui

import com.aquafit.app.data.Exercise
import com.aquafit.app.data.ExerciseMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Fmt {
    private val ptBR: Locale = Locale.forLanguageTag("pt-BR")

    fun num(v: Double): String = String.format(ptBR, "%.2f", v).trimEnd('0').trimEnd(',')

    fun kg(v: Double): String = when {
        v == 0.0 -> "peso corporal"
        v > 0 -> "+${num(v)} kg"
        else -> "${num(v)} kg (assistido)"
    }

    fun duration(sec: Int): String = when {
        sec < 60 -> "${sec}s"
        sec % 60 == 0 -> "${sec / 60}min"
        else -> "${sec / 60}min ${sec % 60}s"
    }

    fun elapsed(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(ptBR, "%d:%02d:%02d", h, m, s)
        else String.format(ptBR, "%02d:%02d", m, s)
    }

    fun dateTime(ms: Long): String =
        SimpleDateFormat("EEE, dd/MM/yyyy 'às' HH:mm", ptBR).format(Date(ms))

    fun shortDate(ms: Long): String = SimpleDateFormat("dd/MM", ptBR).format(Date(ms))

    fun parseDouble(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

    fun parseInt(s: String): Int? = s.trim().toIntOrNull()

    fun amount(mode: ExerciseMode, reps: Int, seconds: Int): String =
        if (mode == ExerciseMode.REPS) "$reps reps" else duration(seconds)

    fun target(e: Exercise): String = "${e.sets} × ${amount(e.mode, e.reps, e.seconds)} · ${kg(e.weightKg)}"
}
