package com.saikat.hold

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/** One swipeable page per [Action]. The page you land on is the saved action. */
class ActionPagerAdapter(
    private val slot: String,
    private val onChoose: (Action) -> Unit,
    private val onAdvanced: () -> Unit
) : RecyclerView.Adapter<ActionPagerAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val title: TextView = v.findViewById(R.id.title)
        val desc: TextView = v.findViewById(R.id.desc)
        val target: TextView = v.findViewById(R.id.target)
        val btnRow: View = v.findViewById(R.id.btnRow)
        val choose: MaterialButton = v.findViewById(R.id.choose)
        val advanced: MaterialButton = v.findViewById(R.id.advanced)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_action_page, parent, false))

    override fun getItemCount() = Action.entries.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val a = Action.entries[position]
        val c = h.itemView.context
        h.icon.setImageResource(a.icon)
        h.title.text = a.title
        h.desc.text = a.desc

        if (a.needsTarget) {
            val t = Slots.target(c, slot, a.id)
            h.target.visibility = View.VISIBLE
            h.target.text = if (t.isEmpty()) "Nothing chosen yet" else ActionRunner.describe(c, a, t)
            h.btnRow.visibility = View.VISIBLE
            h.choose.text = if (t.isEmpty()) "Choose" else "Change"
            h.choose.setOnClickListener { onChoose(a) }

            if (a == Action.ACTIVITY) {
                h.advanced.visibility = View.VISIBLE
                h.advanced.text =
                    if (Slots.advanced(c, slot).isBlank()) "Advanced" else "Advanced •"
                h.advanced.setOnClickListener { onAdvanced() }
            } else {
                h.advanced.visibility = View.GONE
            }
        } else {
            h.target.visibility = View.GONE
            h.btnRow.visibility = View.GONE
        }
    }
}