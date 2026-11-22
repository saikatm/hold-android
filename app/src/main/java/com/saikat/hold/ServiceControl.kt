package com.saikat.hold

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/** Reads and (with the one-time ADB grant) writes the accessibility-service switch. */
object ServiceControl {
    private fun names(c: Context): List<String> {
        val cn = ComponentName(c, KeyService::class.java)
        return listOf(cn.flattenToString(), cn.flattenToShortString())
    }

    private fun current(c: Context): String =
        Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""

    fun isEnabled(c: Context): Boolean {
        val n = names(c)
        return current(c).split(':').any { s -> n.any { it.equals(s, ignoreCase = true) } }
    }

    fun canWrite(c: Context): Boolean =
        c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    /** Returns false when the ADB grant is missing, so the caller can open Settings instead. */
    fun enable(c: Context): Boolean {
        if (!canWrite(c)) return false
        if (isEnabled(c)) return true
        val cur = current(c)
        val me = names(c)[0]
        val updated = if (cur.isEmpty()) me else "$cur:$me"
        Settings.Secure.putString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated)
        Settings.Secure.putInt(c.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        return true
    }

    fun disable(c: Context): Boolean {
        if (!canWrite(c)) return false
        val n = names(c)
        val rest = current(c).split(':').filter { s -> s.isNotEmpty() && n.none { it.equals(s, ignoreCase = true) } }
        Settings.Secure.putString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, rest.joinToString(":"))
        return true
    }
}
