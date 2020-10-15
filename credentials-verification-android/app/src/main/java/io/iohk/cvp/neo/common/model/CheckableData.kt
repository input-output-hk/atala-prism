package io.iohk.cvp.neo.common.model

data class CheckableData<T>(val data: T, private var checked: Boolean = false) {

    val isChecked: Boolean
        get() = this.checked

    fun setChecked(checked: Boolean) {
        this.checked = checked
    }
}