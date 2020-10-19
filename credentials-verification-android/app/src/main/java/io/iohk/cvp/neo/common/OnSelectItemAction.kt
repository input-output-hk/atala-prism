package io.iohk.cvp.neo.common

interface OnSelectItemAction<T, in E : Enum<*>?> {
    fun onSelect(item: T, action: E? = null)
}