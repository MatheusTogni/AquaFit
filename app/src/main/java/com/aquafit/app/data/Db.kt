package com.aquafit.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Db(context: Context) : SQLiteOpenHelper(context, "aquafit.db", null, 2) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE workout (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                position INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        db.execSQL(
            """
            CREATE TABLE exercise (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workout_id INTEGER NOT NULL REFERENCES workout(id) ON DELETE CASCADE,
                position INTEGER NOT NULL,
                name TEXT NOT NULL,
                sets INTEGER NOT NULL,
                mode TEXT NOT NULL,
                reps INTEGER NOT NULL,
                seconds INTEGER NOT NULL,
                weight_kg REAL NOT NULL,
                rest_seconds INTEGER NOT NULL,
                notes TEXT NOT NULL DEFAULT ''
            )
            """
        )
        db.execSQL(
            """
            CREATE TABLE session (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workout_id INTEGER,
                workout_name TEXT NOT NULL,
                started_at INTEGER NOT NULL,
                finished_at INTEGER NOT NULL,
                notes TEXT NOT NULL DEFAULT ''
            )
            """
        )
        db.execSQL(
            """
            CREATE TABLE session_set (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL REFERENCES session(id) ON DELETE CASCADE,
                exercise_name TEXT NOT NULL,
                set_number INTEGER NOT NULL,
                mode TEXT NOT NULL,
                reps INTEGER NOT NULL,
                seconds INTEGER NOT NULL,
                weight_kg REAL NOT NULL,
                done INTEGER NOT NULL
            )
            """
        )
        db.execSQL("CREATE INDEX idx_exercise_workout ON exercise(workout_id, position)")
        db.execSQL("CREATE INDEX idx_set_session ON session_set(session_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Ordem manual dos treinos; começa igual à ordem alfabética que era usada antes.
            db.execSQL("ALTER TABLE workout ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                UPDATE workout SET position = (
                    SELECT COUNT(*) FROM workout w2
                    WHERE w2.name < workout.name COLLATE NOCASE
                       OR (w2.name = workout.name COLLATE NOCASE AND w2.id < workout.id)
                )
                """
            )
        }
    }
}
