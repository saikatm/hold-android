package com.saikat.hold

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.KeyEvent
import android.widget.Toast

/** The pages of the swipe picker. Order here is the swipe order. */
enum class Action(
    val id: String,
    val title: String,
    val desc: String,
    val icon: Int,
    val needsTarget: Boolean = false
) {
    NONE("none", "Do nothing", "The key works like normal.", R.drawable.ic_block),
    ACTIVITY("activity", "Launch activity", "Open a specific screen of an app.", R.drawable.ic_open, true),
    APP("app", "Launch app", "Open an app of your choice.", R.drawable.ic_apps, true),
    FLASHLIGHT("flashlight", "Flashlight", "Toggle the torch on or off.", R.drawable.ic_flashlight),
    CAMERA("camera", "Camera", "Open the default camera app.", R.drawable.ic_camera),
    RINGER("ringer", "Ringer mode", "Switch between ring and vibrate.", R.drawable.ic_vibration),
    MEDIA("media", "Play / pause", "Toggle media playback.", R.drawable.ic_play),
    SCREENSHOT("screenshot", "Screenshot", "Take a screenshot.", R.drawable.ic_screenshot),
    LOCK("lock", "Lock screen", "Turn the screen off and lock.", R.drawable.ic_lock);

    companion object {
        fun byId(id: String): Action = entries.firstOrNull { it.id == id } ?: NONE
    }
}

object ActionRunner {
    @Volatile
    private var torchOn = false
    private var torchWatching = false

    /** Start tracking the torch state so a hold toggles it correctly. */
    fun init(c: Context) {
        if (torchWatching) return
        torchWatching = true
        try {
            val cm = c.getSystemService(CameraManager::class.java)
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    torchOn = enabled
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            torchWatching = false
        }
    }

    /**
     * Builds the intent for Launch activity. [advanced] is optional text, one entry per line:
     *   action=...        data=...        category=...
     *   extra:key=text    extrabool:key=true    extraint:key=5
     */
    private fun activityIntent(target: String, advanced: String): Intent {
        val parts = target.split("/", limit = 2)
        val i = Intent().setClassName(parts[0], parts[1])
        var data: Uri? = null
        for (raw in advanced.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || !line.contains('=')) continue
            val k = line.substringBefore('=').trim()
            val v = line.substringAfter('=').trim()
            when {
                k == "action" -> i.action = v
                k == "data" -> data = Uri.parse(v)
                k == "category" -> i.addCategory(v)
                k.startsWith("extra:") -> i.putExtra(k.removePrefix("extra:"), v)
                k.startsWith("extrabool:") -> i.putExtra(k.removePrefix("extrabool:"), v.toBoolean())
                k.startsWith("extraint:") -> v.toIntOrNull()?.let { i.putExtra(k.removePrefix("extraint:"), it) }
            }
        }
        if (data != null) i.data = data
        return i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun run(s: AccessibilityService, action: Action, target: String, advanced: String = "") {
        try {
            when (action) {
                Action.NONE -> Unit
                Action.FLASHLIGHT -> {
                    val cm = s.getSystemService(CameraManager::class.java)
                    val id = cm.cameraIdList.firstOrNull {
                        cm.getCameraCharacteristics(it)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    } ?: throw IllegalStateException("No flash")
                    cm.setTorchMode(id, !torchOn)
                }
                Action.CAMERA -> s.startActivity(
                    Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Action.RINGER -> {
                    val am = s.getSystemService(AudioManager::class.java)
                    am.ringerMode =
                        if (am.ringerMode == AudioManager.RINGER_MODE_NORMAL)
                            AudioManager.RINGER_MODE_VIBRATE
                        else AudioManager.RINGER_MODE_NORMAL
                }
                Action.MEDIA -> {
                    val am = s.getSystemService(AudioManager::class.java)
                    am.dispatchMediaKeyEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    )
                    am.dispatchMediaKeyEvent(
                        KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    )
                }
                Action.SCREENSHOT ->
                    s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                Action.LOCK ->
                    s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                Action.APP -> {
                    val i = s.packageManager.getLaunchIntentForPackage(target)
                        ?: throw IllegalStateException("No launcher")
                    s.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                Action.ACTIVITY -> s.startActivity(activityIntent(target, advanced))
            }
        } catch (e: Exception) {
            val why = e.message?.take(80) ?: e.javaClass.simpleName
            Toast.makeText(s, "Can't launch ${action.title}: $why", Toast.LENGTH_LONG).show()
        }
    }

    /** Human-readable name of a stored target, for the main screen. */
    fun describe(c: Context, action: Action, target: String): String {
        if (target.isEmpty()) return ""
        val pm = c.packageManager
        return try {
            when (action) {
                Action.APP -> pm.getApplicationInfo(target, 0).loadLabel(pm).toString()
                Action.ACTIVITY -> {
                    val parts = target.split("/", limit = 2)
                    pm.getApplicationInfo(parts[0], 0).loadLabel(pm).toString() +
                        " · " + parts[1].substringAfterLast('.')
                }
                else -> ""
            }
        } catch (e: Exception) {
            target
        }
    }
}