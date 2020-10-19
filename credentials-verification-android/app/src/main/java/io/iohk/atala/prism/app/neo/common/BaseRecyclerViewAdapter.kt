package io.iohk.atala.prism.app.neo.common

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRecyclerViewAdapter<T> : RecyclerView.Adapter<BaseRecyclerViewAdapter.ViewHolder<T>>() {
    private val items: MutableList<T> = mutableListOf()

    override fun onBindViewHolder(holder: ViewHolder<T>, position: Int) {
        holder.beforeBind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
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

    abstract class ViewHolder<M>(view: View) : RecyclerView.ViewHolder(view) {

        protected var data: M? = null

        fun beforeBind(data: M) {
            this.data = data
            bind(data)
        }

        abstract fun bind(data: M)
    }
}