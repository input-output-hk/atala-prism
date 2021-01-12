package io.iohk.atala.prism.app.neo.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

open class SimpleTextRecyclerViewAdapter<T>(private val layoutResource: Int, private val textViewResourceId: Int) : RecyclerView.Adapter<SimpleTextRecyclerViewAdapter.ViewHolder>() {

    private val items: MutableList<T> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(layoutResource, parent, false)
        return ViewHolder(view, textViewResourceId)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(parseItemToString(items[position]))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    protected open fun parseItemToString(item: T): String {
        return item.toString()
    }

    fun add(item: T) {
        this.items.add(item)
    }

    fun addAll(items: Collection<T>) {
        this.items.addAll(items)
    }

    fun clear() {
        items.clear()
    }

    fun remove(item: T) {
        items.remove(item)
    }

    fun removeAll(items: Collection<T>) {
        this.items.removeAll(items)
    }

    class ViewHolder(view: View, textViewResourceId: Int) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(textViewResourceId)
        fun bind(text: String) {
            textView.text = text
        }
    }
}