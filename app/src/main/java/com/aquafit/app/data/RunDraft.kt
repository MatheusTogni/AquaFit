package com.aquafit.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Treino em andamento salvo no aparelho, para retomar se o app for fechado no meio. */
data class RunDraft(
    val workoutId: Long,
    val workoutName: String,
    val startedAt: Long,
    val sets: List<SetState>
) {
    /** Estado de uma série como estava na tela (textos crus, para voltar exatamente igual). */
    data class SetState(val exerciseId: Long, val number: Int, val amount: String, val weight: String, val done: Boolean)

    fun find(exerciseId: Long, number: Int): SetState? = sets.firstOrNull { it.exerciseId == exerciseId && it.number == number }

    companion object {
        private const val PREFS = "run_draft"
        private const val KEY = "draft"

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun load(context: Context): RunDraft? {
            val raw = prefs(context).getString(KEY, null) ?: return null
            return try {
                val o = JSONObject(raw)
                val arr = o.getJSONArray("sets")
                RunDraft(
                    workoutId = o.getLong("workoutId"),
                    workoutName = o.getString("workoutName"),
                    startedAt = o.getLong("startedAt"),
                    sets = List(arr.length()) { i ->
                        val s = arr.getJSONObject(i)
                        SetState(s.getLong("exerciseId"), s.getInt("number"), s.getString("amount"), s.getString("weight"), s.getBoolean("done"))
                    }
                )
            } catch (e: Exception) {
                null
            }
        }

        fun save(context: Context, draft: RunDraft) {
            val arr = JSONArray()
            draft.sets.forEach { s ->
                arr.put(
                    JSONObject()
                        .put("exerciseId", s.exerciseId)
                        .put("number", s.number)
                        .put("amount", s.amount)
                        .put("weight", s.weight)
                        .put("done", s.done)
                )
            }
            val o = JSONObject()
                .put("workoutId", draft.workoutId)
                .put("workoutName", draft.workoutName)
                .put("startedAt", draft.startedAt)
                .put("sets", arr)
            prefs(context).edit().putString(KEY, o.toString()).apply()
        }

        fun clear(context: Context) {
            prefs(context).edit().remove(KEY).apply()
        }
    }
}
