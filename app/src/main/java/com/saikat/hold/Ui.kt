package com.saikat.hold

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object Ui {
    /** Draw edge to edge on black and pad [root] by the system bars. */
    fun edge(a: Activity, root: View) {
        WindowCompat.setDecorFitsSystemWindows(a.window, false)
        val l = root.paddingLeft
        val t = root.paddingTop
        val r = root.paddingRight
        val b = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, w ->
            val bars = w.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(l + bars.left, t + bars.top, r + bars.right, b + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()
}
