package com.saikat.hold

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Watches the volume keys. A hold runs the slot's action; a short press is replayed
 * as a normal volume step. Keys with no action assigned are never touched.
 */
class KeyService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var pendingCode = -1
    private var fired = false

    override fun onServiceConnected() {
        ActionRunner.init(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        cancel()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cancel()
        return super.onUnbind(intent)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) return false

        val slot = if (code == KeyEvent.KEYCODE_VOLUME_UP) Slots.UP else Slots.DOWN
        val action = Action.byId(Slots.type(this, slot))
        val target = Slots.target(this, slot, action.id)

        // Nothing assigned (or a launch action without a target): leave the key alone.
        if (action == Action.NONE || (action.needsTarget && target.isEmpty())) return false
        if (Slots.screenOnOnly(this) && !getSystemService(PowerManager::class.java).isInteractive) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    cancel()
                    pendingCode = code
                    fired = false
                    val r = Runnable {
                        fired = true
                        buzz()
                        ActionRunner.run(this, action, target)
                    }
                    pending = r
                    handler.postDelayed(r, Slots.holdMs(this).toLong())
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                val wasPending = pendingCode == code
                val wasFired = fired
                cancel()
                if (wasPending && !wasFired) adjustVolume(code)
                return true
            }
        }
        return false
    }

    private fun cancel() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
        pendingCode = -1
    }

    private fun adjustVolume(code: Int) {
        val am = getSystemService(AudioManager::class.java)
        am.adjustSuggestedStreamVolume(
            if (code == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun buzz() {
        if (!Slots.vibration(this)) return
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
    }
}
