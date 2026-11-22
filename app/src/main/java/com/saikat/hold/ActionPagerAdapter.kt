package com.saikat.hold

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/**
 * One page per [Action], scrolled vertically. Scrolling only previews a page;
 * tapping the big icon confirms it and saves it as the slot's action.
 */
class ActionPagerAdapter(
    private val slot: String,
    private val onChoose: (Action) -> Unit
) : RecyclerView.Adapter<ActionPagerAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val active: TextView = v.findViewById(R.id.active)
        val icon: ImageView = v.findViewById(R.id.icon)
        val title: TextView = v.findViewById(R.id.title)
        val target: TextView = v.findViewById(R.id.target)
        val choose: MaterialButton = v.findViewById(R.id.choose)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_action_page, parent, false))

    override fun getItemCount() = Action.entries.size

    private fun showActive(h: VH, saved: Boolean) {
        val c = h.itemView.context
        h.active.text = if (saved) "● Active" else "Tap icon to set"
        h.active.setTextColor(c.getColor(if (saved) R.color.white else R.color.secondary))
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val a = Action.entries[position]
        val c = h.itemView.context
        h.icon.setImageResource(a.icon)
        h.title.text = a.title
        showActive(h, Slots.type(c, slot) == a.id)

        h.icon.setOnClickListener {
            if (a.needsTarget && Slots.target(c, slot, a.id).isEmpty()) {
                Toast.makeText(c, "Choose one first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val prev = Action.entries.indexOf(Action.byId(Slots.type(c, slot)))
            Slots.setType(c, slot, a.id)
            showActive(h, true)
            if (prev != position) notifyItemChanged(prev)

            h.icon.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.LONG_PRESS
            )
            // Flash a check so the save is obvious, then put the action icon back.
            val pos = h.adapterPosition
            h.icon.setImageResource(R.drawable.ic_check)
            h.icon.postDelayed({
                if (h.adapterPosition == pos) h.icon.setImageResource(a.icon)
            }, 700)
        }

        if (a.needsTarget) {
            val t = Slots.target(c, slot, a.id)
            h.target.visibility = View.VISIBLE
            h.target.text = when {
                t.isEmpty() -> "Nothing chosen yet"
                a == Action.SHORTCUT ->
                    Slots.target(c, slot, Slots.SHORTCUT_NAME).ifEmpty { "Shortcut" }
                else -> ActionRunner.describe(c, a, t)
            }
            h.choose.visibility = View.VISIBLE
            h.choose.text = if (t.isEmpty()) "Choose" else "Change"
            h.choose.setOnClickListener { onChoose(a) }
        } else {
            h.target.visibility = View.GONE
            h.choose.visibility = View.GONE
        }
    }
}