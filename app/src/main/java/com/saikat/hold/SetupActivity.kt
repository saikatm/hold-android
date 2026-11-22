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
import android.widget.ImageView
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

        add(R.drawable.ic_accessibility, "Turn on the Hold service", null, ServiceControl.isEnabled(this)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        add(R.drawable.ic_battery, "Battery: unrestricted", null, pm.isIgnoringBatteryOptimizations(packageName)) {
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
            R.drawable.ic_pause, "Pause app activity if unused: off",
            "Opens App info. Turn that switch off.", Slots.flag(this, "pause")
        ) {
            Slots.setFlag(this, "pause")
            openAppInfo()
        }

        add(
            R.drawable.ic_lock_open, "Allow restricted settings",
            "Only if you sideloaded the APK. In App info tap the three dots, then Allow restricted settings.",
            Slots.flag(this, "restricted")
        ) {
            Slots.setFlag(this, "restricted")
            openAppInfo()
        }

        val cmd = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
        add(
            R.drawable.ic_terminal, "Grant secure settings (optional)",
            "Tap to copy the ADB command. Lets Turn on / Turn off work without opening Settings.",
            ServiceControl.canWrite(this)
        ) {
            val cb = getSystemService(ClipboardManager::class.java)
            cb.setPrimaryClip(ClipData.newPlainText("adb", cmd))
            Toast.makeText(this, "Command copied", Toast.LENGTH_SHORT).show()
        }

        oemIntent()?.let { oem ->
            add(
                R.drawable.ic_rocket, "Allow autostart",
                "Your phone brand may kill background apps.", Slots.flag(this, "autostart")
            ) {
                Slots.setFlag(this, "autostart")
                try {
                    startActivity(oem)
                } catch (e: Exception) {
                    openAppInfo()
                }
            }
        }
    }

    /** One tappable card: icon, title, optional note, and a chevron (todo) or check (done). */
    private fun add(icon: Int, title: String, note: String?, done: Boolean, onClick: () -> Unit) {
        val v = layoutInflater.inflate(R.layout.item_step, steps, false)
        v.findViewById<ImageView>(R.id.stepIcon).setImageResource(icon)
        v.findViewById<TextView>(R.id.stepTitle).text = title
        if (note != null) {
            val n = v.findViewById<TextView>(R.id.stepNote)
            n.text = note
            n.visibility = View.VISIBLE
        }
        v.findViewById<ImageView>(R.id.stepStatus)
            .setImageResource(if (done) R.drawable.ic_check_circle else R.drawable.ic_chevron)
        v.alpha = if (done) 0.55f else 1f
        v.setOnClickListener { onClick() }
        steps.addView(v)
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