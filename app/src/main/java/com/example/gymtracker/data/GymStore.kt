package com.example.gymtracker.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** SharedPreferences-backed persistence; the Android equivalent of the web app's localStorage. */
class GymStore(context: Context) {
    private val prefs = context.getSharedPreferences("gym", Context.MODE_PRIVATE)

    fun loadUnit(): WeightUnit = WeightUnit.fromLabel(prefs.getString(KEY_UNIT, null))
    fun saveUnit(unit: WeightUnit) = prefs.edit { putString(KEY_UNIT, unit.label) }

    fun loadWorkouts(): List<Workout> = readArray(KEY_WORKOUTS).mapObjects { it.toWorkout() }
    fun saveWorkouts(workouts: List<Workout>) =
        prefs.edit { putString(KEY_WORKOUTS, JSONArray(workouts.map { it.toJson() }).toString()) }

    fun loadPresets(): List<PresetWorkout> = readArray(KEY_PRESETS).mapObjects { it.toPresetWorkout() }
    fun savePresets(presets: List<PresetWorkout>) =
        prefs.edit { putString(KEY_PRESETS, JSONArray(presets.map { it.toJson() }).toString()) }

    fun loadActiveWorkout(): ActiveWorkout? = prefs.getString(KEY_ACTIVE, null)?.let {
        try {
            JSONObject(it).toActiveWorkout()
        } catch (_: JSONException) {
            null
        }
    }

    fun saveActiveWorkout(active: ActiveWorkout?) = prefs.edit {
        if (active == null) remove(KEY_ACTIVE) else putString(KEY_ACTIVE, active.toJson().toString())
    }

    private fun readArray(key: String): JSONArray? = prefs.getString(key, null)?.let {
        try {
            JSONArray(it)
        } catch (_: JSONException) {
            null
        }
    }

    private companion object {
        const val KEY_UNIT = "gym-unit"
        const val KEY_WORKOUTS = "gym-workouts"
        const val KEY_PRESETS = "gym-presets"
        const val KEY_ACTIVE = "gym-active-workout"
    }
}
