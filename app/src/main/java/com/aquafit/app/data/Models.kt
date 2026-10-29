package com.aquafit.app.data

enum class ExerciseMode { REPS, TIME }

data class Workout(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val exerciseCount: Int = 0
)

data class Exercise(
    val id: Long = 0,
    val workoutId: Long,
    val position: Int = 0,
    val name: String,
    val sets: Int = 3,
    val mode: ExerciseMode = ExerciseMode.REPS,
    val reps: Int = 10,
    val seconds: Int = 30,
    val weightKg: Double = 0.0,
    val restSeconds: Int = 60,
    val notes: String = ""
)

data class Session(
    val id: Long = 0,
    val workoutId: Long?,
    val workoutName: String,
    val startedAt: Long,
    val finishedAt: Long,
    val notes: String = "",
    val doneSets: Int = 0,
    val totalSets: Int = 0
)

data class SessionSet(
    val id: Long = 0,
    val sessionId: Long = 0,
    val exerciseName: String,
    val setNumber: Int,
    val mode: ExerciseMode,
    val reps: Int,
    val seconds: Int,
    val weightKg: Double,
    val done: Boolean
)
