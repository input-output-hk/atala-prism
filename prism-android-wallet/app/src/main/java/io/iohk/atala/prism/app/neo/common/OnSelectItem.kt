package io.iohk.atala.prism.app.neo.common

interface OnSelectItem<T> {
    fun onSelect(item: T)
}