package com.aquafit.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class Repository private constructor(context: Context) {

    private val db = Db(context.applicationContext)

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context).also { instance = it }
            }
    }

    // ---------- Treinos ----------

    fun workouts(): List<Workout> =
        db.readableDatabase.rawQuery(
            """
            SELECT w.id, w.name, w.description, w.created_at,
                   (SELECT COUNT(*) FROM exercise e WHERE e.workout_id = w.id)
            FROM workout w
            ORDER BY w.position, w.id
            """,
            null
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(Workout(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getInt(4)))
                }
            }
        }

    fun workout(id: Long): Workout? =
        db.readableDatabase.rawQuery(
            "SELECT id, name, description, created_at FROM workout WHERE id = ?",
            arrayOf(id.toString())
        ).use { c ->
            if (c.moveToFirst()) Workout(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)) else null
        }

    fun saveWorkout(w: Workout): Long {
        val values = ContentValues().apply {
            put("name", w.name)
            put("description", w.description)
        }
        val wdb = db.writableDatabase
        return if (w.id == 0L) {
            val next = wdb.rawQuery("SELECT COALESCE(MAX(position), -1) + 1 FROM workout", null)
                .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
            values.put("created_at", w.createdAt)
            values.put("position", next)
            wdb.insertOrThrow("workout", null, values)
        } else {
            wdb.update("workout", values, "id = ?", arrayOf(w.id.toString()))
            w.id
        }
    }

    fun deleteWorkout(id: Long) {
        db.writableDatabase.delete("workout", "id = ?", arrayOf(id.toString()))
    }

    /** Regrava as posições dos treinos na ordem da lista recebida. */
    fun reorderWorkouts(ordered: List<Workout>) = reorder("workout", ordered.map { it.id })

    // ---------- Exercícios ----------

    fun exercises(workoutId: Long): List<Exercise> =
        db.readableDatabase.rawQuery(
            """
            SELECT id, workout_id, position, name, sets, mode, reps, seconds, weight_kg, rest_seconds, notes
            FROM exercise WHERE workout_id = ? ORDER BY position, id
            """,
            arrayOf(workoutId.toString())
        ).use { c ->
            buildList { while (c.moveToNext()) add(c.toExercise()) }
        }

    private fun Cursor.toExercise() = Exercise(
        id = getLong(0),
        workoutId = getLong(1),
        position = getInt(2),
        name = getString(3),
        sets = getInt(4),
        mode = ExerciseMode.valueOf(getString(5)),
        reps = getInt(6),
        seconds = getInt(7),
        weightKg = getDouble(8),
        restSeconds = getInt(9),
        notes = getString(10)
    )

    fun saveExercise(e: Exercise): Long {
        val values = ContentValues().apply {
            put("workout_id", e.workoutId)
            put("name", e.name)
            put("sets", e.sets)
            put("mode", e.mode.name)
            put("reps", e.reps)
            put("seconds", e.seconds)
            put("weight_kg", e.weightKg)
            put("rest_seconds", e.restSeconds)
            put("notes", e.notes)
        }
        val wdb = db.writableDatabase
        return if (e.id == 0L) {
            val next = wdb.rawQuery(
                "SELECT COALESCE(MAX(position), -1) + 1 FROM exercise WHERE workout_id = ?",
                arrayOf(e.workoutId.toString())
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
            values.put("position", next)
            wdb.insertOrThrow("exercise", null, values)
        } else {
            wdb.update("exercise", values, "id = ?", arrayOf(e.id.toString()))
            e.id
        }
    }

    fun deleteExercise(id: Long) {
        db.writableDatabase.delete("exercise", "id = ?", arrayOf(id.toString()))
    }

    /** Regrava as posições na ordem da lista recebida. */
    fun reorderExercises(ordered: List<Exercise>) = reorder("exercise", ordered.map { it.id })

    private fun reorder(table: String, ids: List<Long>) {
        val wdb = db.writableDatabase
        wdb.beginTransaction()
        try {
            ids.forEachIndexed { index, id ->
                val v = ContentValues().apply { put("position", index) }
                wdb.update(table, v, "id = ?", arrayOf(id.toString()))
            }
            wdb.setTransactionSuccessful()
        } finally {
            wdb.endTransaction()
        }
    }

    // ---------- Sessões (histórico) ----------

    fun saveSession(session: Session, sets: List<SessionSet>): Long {
        val wdb = db.writableDatabase
        wdb.beginTransaction()
        try {
            val sv = ContentValues().apply {
                if (session.workoutId != null) put("workout_id", session.workoutId) else putNull("workout_id")
                put("workout_name", session.workoutName)
                put("started_at", session.startedAt)
                put("finished_at", session.finishedAt)
                put("notes", session.notes)
            }
            val sessionId = wdb.insertOrThrow("session", null, sv)
            for (s in sets) {
                val v = ContentValues().apply {
                    put("session_id", sessionId)
                    put("exercise_name", s.exerciseName)
                    put("set_number", s.setNumber)
                    put("mode", s.mode.name)
                    put("reps", s.reps)
                    put("seconds", s.seconds)
                    put("weight_kg", s.weightKg)
                    put("done", if (s.done) 1 else 0)
                }
                wdb.insertOrThrow("session_set", null, v)
            }
            wdb.setTransactionSuccessful()
            return sessionId
        } finally {
            wdb.endTransaction()
        }
    }

    private val sessionSelect = """
        SELECT s.id, s.workout_id, s.workout_name, s.started_at, s.finished_at, s.notes,
               (SELECT COUNT(*) FROM session_set x WHERE x.session_id = s.id AND x.done = 1),
               (SELECT COUNT(*) FROM session_set x WHERE x.session_id = s.id)
        FROM session s
    """

    fun sessions(): List<Session> =
        db.readableDatabase.rawQuery("$sessionSelect ORDER BY s.started_at DESC", null).use { c ->
            buildList { while (c.moveToNext()) add(c.toSession()) }
        }

    fun session(id: Long): Session? =
        db.readableDatabase.rawQuery("$sessionSelect WHERE s.id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) c.toSession() else null
        }

    private fun Cursor.toSession() = Session(
        id = getLong(0),
        workoutId = if (isNull(1)) null else getLong(1),
        workoutName = getString(2),
        startedAt = getLong(3),
        finishedAt = getLong(4),
        notes = getString(5),
        doneSets = getInt(6),
        totalSets = getInt(7)
    )

    fun sessionSets(sessionId: Long): List<SessionSet> =
        db.readableDatabase.rawQuery(
            """
            SELECT id, session_id, exercise_name, set_number, mode, reps, seconds, weight_kg, done
            FROM session_set WHERE session_id = ? ORDER BY id
            """,
            arrayOf(sessionId.toString())
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        SessionSet(
                            id = c.getLong(0),
                            sessionId = c.getLong(1),
                            exerciseName = c.getString(2),
                            setNumber = c.getInt(3),
                            mode = ExerciseMode.valueOf(c.getString(4)),
                            reps = c.getInt(5),
                            seconds = c.getInt(6),
                            weightKg = c.getDouble(7),
                            done = c.getInt(8) == 1
                        )
                    )
                }
            }
        }

    fun deleteSession(id: Long) {
        db.writableDatabase.delete("session", "id = ?", arrayOf(id.toString()))
    }

    /** Data da última sessão do treino (para mostrar na lista). */
    fun lastSessionAt(workoutId: Long): Long? =
        db.readableDatabase.rawQuery(
            "SELECT MAX(started_at) FROM session WHERE workout_id = ?",
            arrayOf(workoutId.toString())
        ).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null }
}
