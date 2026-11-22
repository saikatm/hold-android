package com.saikat.hold

import android.content.Context

/** All settings live here: two slots (volume up / down) plus a few switches. */
object Slots {
    const val UP = "up"
    const val DOWN = "down"

    /** Pseudo action id used to store the display name of the chosen app shortcut. */
    const val SHORTCUT_NAME = "shortcut_name"

    private fun p(c: Context) = c.getSharedPreferences("hold", Context.MODE_PRIVATE)

    fun type(c: Context, slot: String): String =
        p(c).getString("${slot}_type", Action.NONE.id) ?: Action.NONE.id

    fun setType(c: Context, slot: String, id: String) {
        p(c).edit().putString("${slot}_type", id).apply()
    }

    /** Target is stored per slot and per action type, so switching pages keeps the old choice. */
    fun target(c: Context, slot: String, id: String): String =
        p(c).getString("${slot}_${id}_target", "") ?: ""

    fun setTarget(c: Context, slot: String, id: String, target: String) {
        p(c).edit().putString("${slot}_${id}_target", target).apply()
    }

    fun vibration(c: Context) = p(c).getBoolean("vibration", true)
    fun setVibration(c: Context, v: Boolean) = p(c).edit().putBoolean("vibration", v).apply()

    fun screenOnOnly(c: Context) = p(c).getBoolean("screen_on_only", true)
    fun setScreenOnOnly(c: Context, v: Boolean) = p(c).edit().putBoolean("screen_on_only", v).apply()

    fun holdMs(c: Context) = p(c).getInt("hold_ms", 400)
    fun setHoldMs(c: Context, v: Int) = p(c).edit().putInt("hold_ms", v).apply()

    fun setupDone(c: Context) = p(c).getBoolean("setup_done", false)
    fun markSetupDone(c: Context) = p(c).edit().putBoolean("setup_done", true).apply()

    fun flag(c: Context, name: String) = p(c).getBoolean("flag_$name", false)
    fun setFlag(c: Context, name: String) = p(c).edit().putBoolean("flag_$name", true).apply()
}