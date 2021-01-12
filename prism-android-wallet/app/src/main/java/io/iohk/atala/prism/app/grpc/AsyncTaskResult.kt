package io.iohk.atala.prism.app.grpc

class AsyncTaskResult<A> {
    var result: A? = null
        private set
    var error: Exception? = null
        private set

    constructor() {}
    constructor(result: A) {
        this.result = result
    }

    constructor(error: Exception?) {
        this.error = error
    }
}
