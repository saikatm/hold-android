package com.saikat.hold

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/** Shown once on first launch. Every step is optional; "Continue" is always enabled. */
class SetupActivity : AppCompatActivity() {

    private lateinit var steps: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        Ui.edge(this, findViewById(R.id.root))
        steps = findViewById(R.id.steps)
        findViewById<View>(R.id.btnContinue).setOnClickListener { finish() }
        Slots.markSetupDone(this)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun build() {
        steps.removeAllViews()
        val pm = getSystemService(PowerManager::class.java)

        add("Turn on the Hold service", ServiceControl.isEnabled(this), null) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        add("Battery: unrestricted", pm.isIgnoringBatteryOptimizations(packageName), null) {
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        add(
            "Pause app activity if unused: off", Slots.flag(this, "pause"),
            "Opens App info. Turn off \"Pause app activity if unused\"."
        ) {
            Slots.setFlag(this, "pause")
            openAppInfo()
        }

        add(
            "Allow restricted settings", Slots.flag(this, "restricted"),
            "Only if you sideloaded the APK. In App info tap the three dots, then Allow restricted settings."
        ) {
            Slots.setFlag(this, "restricted")
            openAppInfo()
        }

        val cmd = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
        add(
            "Grant secure settings (optional)", ServiceControl.canWrite(this),
            "Lets the Turn on / Turn off button work without opening Settings. Tap to copy the ADB command:\n$cmd"
        ) {
            val cb = getSystemService(ClipboardManager::class.java)
            cb.setPrimaryClip(ClipData.newPlainText("adb", cmd))
            Toast.makeText(this, "Command copied", Toast.LENGTH_SHORT).show()
        }

        oemIntent()?.let { oem ->
            add("Allow autostart", Slots.flag(this, "autostart"), "Your phone brand may kill background apps.") {
                Slots.setFlag(this, "autostart")
                try {
                    startActivity(oem)
                } catch (e: Exception) {
                    openAppInfo()
                }
            }
        }
    }

    private fun add(title: String, done: Boolean, note: String?, onClick: () -> Unit) {
        val styled = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        styled.text = title
        styled.isAllCaps = false
        styled.setTextColor(0xFFFFFFFF.toInt())
        styled.cornerRadius = Ui.dp(this, 8)
        styled.setStrokeColorResource(R.color.outline)
        styled.strokeWidth = Ui.dp(this, 1)
        if (done) {
            styled.setIconResource(R.drawable.ic_check)
            styled.iconTint = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
        }
        styled.setOnClickListener { onClick() }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 56)
        )
        lp.topMargin = Ui.dp(this, 12)
        steps.addView(styled, lp)

        if (note != null) {
            val tv = TextView(this)
            tv.text = note
            tv.setTextColor(resources.getColor(R.color.secondary, theme))
            tv.textSize = 12f
            val nlp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            nlp.topMargin = Ui.dp(this, 4)
            steps.addView(tv, nlp)
        }
    }

    private fun openAppInfo() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:$packageName"))
        )
    }

    /** Known autostart screens for a few phone brands; null on stock Android. */
    private fun oemIntent(): Intent? {
        val c: ComponentName = when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi", "poco" ->
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            "oppo", "realme", "oneplus" ->
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
            "vivo", "iqoo" ->
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            "huawei", "honor" ->
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            "samsung" ->
                ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
            else -> return null
        }
        return Intent().setComponent(c)
    }
}
