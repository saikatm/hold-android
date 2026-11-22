package com.saikat.hold

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Picks the target for a Launch app / Launch activity slot and saves it directly.
 * App mode: tap an app. Activity mode: tap an app to expand its exported activities, tap one.
 */
class PickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SLOT = "slot"
        const val EXTRA_MODE = "mode"
    }

    private class AppItem(val label: String, val pkg: String, val info: ResolveInfo)

    /** One visible row: either an app, or one of an expanded app's activities. */
    private class Row(val app: AppItem, val activity: String?, val activityLabel: String?)

    private lateinit var slot: String
    private lateinit var mode: Action
    private var apps: List<AppItem> = emptyList()
    private val expanded = mutableSetOf<String>()
    private val activityCache = mutableMapOf<String, List<Pair<String, String>>>()
    private var rows: List<Row> = emptyList()
    private var query = ""
    private lateinit var adapter: RowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        Ui.edge(this, findViewById(R.id.root))

        slot = intent.getStringExtra(EXTRA_SLOT) ?: Slots.UP
        mode = Action.byId(intent.getStringExtra(EXTRA_MODE) ?: Action.APP.id)

        findViewById<TextView>(R.id.title).text =
            if (mode == Action.ACTIVITY) "Choose activity" else "Choose app"
        findViewById<View>(R.id.back).setOnClickListener { finish() }

        adapter = RowAdapter()
        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim() ?: ""
                rebuild()
            }
        })

        Thread {
            val loaded = loadApps()
            runOnUiThread {
                apps = loaded
                rebuild()
            }
        }.start()
    }

    private fun loadApps(): List<AppItem> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val found: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return found
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it) }
            .sortedBy { it.label.lowercase() }
    }

    @Suppress("DEPRECATION")
    private fun activitiesOf(pkg: String): List<Pair<String, String>> =
        activityCache.getOrPut(pkg) {
            try {
                val pm = packageManager
                val info = if (Build.VERSION.SDK_INT >= 33) {
                    pm.getPackageInfo(
                        pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
                    )
                } else {
                    pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
                }
                info.activities.orEmpty()
                    .filter { it.exported && it.enabled && it.permission == null }
                    .map { it.name to it.loadLabel(pm).toString() }
                    .sortedBy { it.first }
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun rebuild() {
        val q = query.lowercase()
        val out = ArrayList<Row>()
        for (app in apps) {
            if (q.isNotEmpty() && !app.label.lowercase().contains(q) && !app.pkg.lowercase().contains(q)) continue
            out.add(Row(app, null, null))
            if (mode == Action.ACTIVITY && expanded.contains(app.pkg)) {
                for ((cls, label) in activitiesOf(app.pkg)) out.add(Row(app, cls, label))
            }
        }
        rows = out
        adapter.notifyDataSetChanged()
    }

    private fun save(target: String) {
        Slots.setTarget(this, slot, mode.id, target)
        finish()
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.icon)
            val name: TextView = v.findViewById(R.id.name)
            val sub: TextView = v.findViewById(R.id.sub)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(h: VH, position: Int) {
            val r = rows[position]
            val pad = h.itemView
            if (r.activity == null) {
                h.icon.visibility = View.VISIBLE
                h.icon.setImageDrawable(r.app.info.loadIcon(packageManager))
                h.name.text = r.app.label
                h.sub.text = if (mode == Action.ACTIVITY) {
                    if (expanded.contains(r.app.pkg)) "Tap to collapse" else "Tap to show activities"
                } else r.app.pkg
                pad.setPaddingRelative(Ui.dp(this@PickerActivity, 24), pad.paddingTop, pad.paddingEnd, pad.paddingBottom)
                pad.setOnClickListener {
                    if (mode == Action.ACTIVITY) {
                        if (!expanded.add(r.app.pkg)) expanded.remove(r.app.pkg)
                        rebuild()
                    } else {
                        save(r.app.pkg)
                    }
                }
            } else {
                h.icon.visibility = View.GONE
                h.name.text = r.activityLabel
                h.sub.text = r.activity
                pad.setPaddingRelative(Ui.dp(this@PickerActivity, 80), pad.paddingTop, pad.paddingEnd, pad.paddingBottom)
                pad.setOnClickListener { save("${r.app.pkg}/${r.activity}") }
            }
        }
    }
}
