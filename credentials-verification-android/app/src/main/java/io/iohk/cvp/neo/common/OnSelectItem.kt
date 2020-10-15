package io.iohk.cvp.neo.common

interface OnSelectItem<T> {
    fun onSelect(item: T)
}