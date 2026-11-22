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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Picks the target for a slot.
 *  - Launch app: tap an app, its package is saved.
 *  - Launch app shortcut: lists every shortcut apps offer (the same list Button Mapper shows).
 *    Tapping one asks that app to create the shortcut, and Hold saves the intent it returns.
 */
class PickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SLOT = "slot"
        const val EXTRA_MODE = "mode"
    }

    private class Item(
        val label: String,
        val sub: String,
        val pkg: String,
        val cls: String?,
        val info: ResolveInfo
    )

    private lateinit var slot: String
    private lateinit var mode: Action
    private var items: List<Item> = emptyList()
    private var shown: List<Item> = emptyList()
    private var query = ""
    private var pendingLabel = ""
    private lateinit var adapter: RowAdapter

    private val shortcutResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val data = r.data
            if (r.resultCode != RESULT_OK || data == null) return@registerForActivityResult

            @Suppress("DEPRECATION")
            val shortcut: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
            } else {
                data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)
            }
            if (shortcut == null) {
                Toast.makeText(this, "That shortcut didn't return anything to run", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            val name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)?.takeIf { it.isNotBlank() }
                ?: pendingLabel
            Slots.setTarget(this, slot, Action.SHORTCUT.id, shortcut.toUri(Intent.URI_INTENT_SCHEME))
            Slots.setTarget(this, slot, Slots.SHORTCUT_NAME, name)
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        Ui.edge(this, findViewById(R.id.root))

        slot = intent.getStringExtra(EXTRA_SLOT) ?: Slots.UP
        mode = Action.byId(intent.getStringExtra(EXTRA_MODE) ?: Action.APP.id)

        findViewById<TextView>(R.id.title).text =
            if (mode == Action.SHORTCUT) "Choose shortcut" else "Choose app"
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
                filter()
            }
        })

        Thread {
            val loaded = load()
            runOnUiThread {
                items = loaded
                filter()
            }
        }.start()
    }

    private fun query(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

    private fun load(): List<Item> {
        val pm = packageManager
        return if (mode == Action.SHORTCUT) {
            query(Intent(Intent.ACTION_CREATE_SHORTCUT))
                .filter { it.activityInfo.packageName != packageName }
                .map {
                    val app = it.activityInfo.applicationInfo.loadLabel(pm).toString()
                    Item(it.loadLabel(pm).toString(), app, it.activityInfo.packageName, it.activityInfo.name, it)
                }
                .sortedWith(compareBy({ it.sub.lowercase() }, { it.label.lowercase() }))
        } else {
            query(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
                .filter { it.activityInfo.packageName != packageName }
                .distinctBy { it.activityInfo.packageName }
                .map { Item(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.packageName, null, it) }
                .sortedBy { it.label.lowercase() }
        }
    }

    private fun filter() {
        val q = query.lowercase()
        shown = if (q.isEmpty()) items else items.filter {
            it.label.lowercase().contains(q) || it.sub.lowercase().contains(q)
        }
        adapter.notifyDataSetChanged()
    }

    private fun pick(item: Item) {
        if (mode == Action.SHORTCUT) {
            pendingLabel = item.label
            try {
                shortcutResult.launch(
                    Intent(Intent.ACTION_CREATE_SHORTCUT).setClassName(item.pkg, item.cls!!)
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Can't open that shortcut", Toast.LENGTH_SHORT).show()
            }
        } else {
            Slots.setTarget(this, slot, mode.id, item.pkg)
            finish()
        }
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.icon)
            val name: TextView = v.findViewById(R.id.name)
            val sub: TextView = v.findViewById(R.id.sub)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

        override fun getItemCount() = shown.size

        override fun onBindViewHolder(h: VH, position: Int) {
            val item = shown[position]
            h.icon.setImageDrawable(item.info.loadIcon(packageManager))
            h.name.text = item.label
            h.sub.text = item.sub
            h.itemView.setOnClickListener { pick(item) }
        }
    }
}