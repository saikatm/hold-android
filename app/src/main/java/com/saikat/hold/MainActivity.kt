package com.saikat.hold

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var headline: TextView
    private lateinit var sub: TextView
    private lateinit var btnOn: MaterialButton
    private lateinit var btnOff: MaterialButton
    private lateinit var adapterUp: ActionPagerAdapter
    private lateinit var adapterDown: ActionPagerAdapter

    private val holdIds = mapOf(
        R.id.h300 to 300, R.id.h400 to 400, R.id.h500 to 500, R.id.h650 to 650, R.id.h800 to 800
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Ui.edge(this, findViewById(R.id.root))

        headline = findViewById(R.id.headline)
        sub = findViewById(R.id.sub)
        btnOn = findViewById(R.id.btnOn)
        btnOff = findViewById(R.id.btnOff)

        btnOn.setOnClickListener {
            if (!ServiceControl.enable(this)) openAccessibilitySettings()
            refreshStatus()
        }
        btnOff.setOnClickListener {
            if (!ServiceControl.disable(this)) openAccessibilitySettings()
            refreshStatus()
        }

        adapterUp = setupPane(Slots.UP, R.id.pagerUp, R.id.dotsUp)
        adapterDown = setupPane(Slots.DOWN, R.id.pagerDown, R.id.dotsDown)

        // Hold time chips
        val group = findViewById<MaterialButtonToggleGroup>(R.id.holdGroup)
        val saved = Slots.holdMs(this)
        val savedId = holdIds.entries.firstOrNull { it.value == saved }?.key ?: R.id.h400
        group.check(savedId)
        group.addOnButtonCheckedListener { _, id, checked ->
            if (checked) holdIds[id]?.let { Slots.setHoldMs(this, it) }
        }

        // Switches
        val swVib = findViewById<MaterialSwitch>(R.id.swVibration)
        swVib.isChecked = Slots.vibration(this)
        swVib.setOnCheckedChangeListener { _, on -> Slots.setVibration(this, on) }

        val swScreen = findViewById<MaterialSwitch>(R.id.swScreen)
        swScreen.isChecked = Slots.screenOnOnly(this)
        swScreen.setOnCheckedChangeListener { _, on -> Slots.setScreenOnOnly(this, on) }

        findViewById<View>(R.id.btnSetup).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        val ver = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            ""
        }
        findViewById<TextView>(R.id.version).text = "Hold $ver"

        if (!Slots.setupDone(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        adapterUp.notifyDataSetChanged()
        adapterDown.notifyDataSetChanged()
    }

    private fun refreshStatus() {
        if (ServiceControl.isEnabled(this)) {
            headline.text = "Ready"
            sub.text = "Hold a volume key to run its action."
            btnOn.visibility = View.GONE
            btnOff.visibility = View.VISIBLE
        } else {
            headline.text = "Setup"
            sub.text = "Turn on the Hold service to start."
            btnOn.visibility = View.VISIBLE
            btnOff.visibility = View.GONE
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun setupPane(slot: String, pagerId: Int, dotsId: Int): ActionPagerAdapter {
        val pager = findViewById<ViewPager2>(pagerId)
        val dots = findViewById<LinearLayout>(dotsId)
        val adapter = ActionPagerAdapter(slot) { action ->
            startActivity(
                Intent(this, PickerActivity::class.java)
                    .putExtra(PickerActivity.EXTRA_SLOT, slot)
                    .putExtra(PickerActivity.EXTRA_MODE, action.id)
            )
        }
        pager.orientation = ViewPager2.ORIENTATION_VERTICAL
        keepScrollInsidePager(pager)
        pager.adapter = adapter

        for (i in Action.entries.indices) dots.addView(makeDot())

        var current = Action.entries.indexOf(Action.byId(Slots.type(this, slot)))
        pager.setCurrentItem(current, false)
        showDot(dots, current)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                showDot(dots, position)
                if (position != current) {
                    current = position
                    Slots.setType(this@MainActivity, slot, Action.entries[position].id)
                    pager.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
        })
        return adapter
    }

    /** The page lives inside a vertically scrolling screen; stop the screen stealing the swipe. */
    private fun keepScrollInsidePager(pager: ViewPager2) {
        (pager.getChildAt(0) as? RecyclerView)?.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    private fun makeDot(): View {
        val size = Ui.dp(this, 6)
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(size, size).apply {
            setMargins(Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 4))
        }
        v.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
        }
        return v
    }

    private fun showDot(dots: LinearLayout, index: Int) {
        for (i in 0 until dots.childCount) {
            dots.getChildAt(i).alpha = if (i == index) 1f else 0.25f
        }
    }
}