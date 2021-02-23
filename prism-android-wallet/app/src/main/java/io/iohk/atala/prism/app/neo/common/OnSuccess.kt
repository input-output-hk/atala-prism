package io.iohk.atala.prism.app.neo.common

interface OnSuccess<T> {
    fun onSuccess(data: T)
}
