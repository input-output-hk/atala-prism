package io.iohk.atala.prism.app.neo.model

enum class DashboardNotification(val identifierCode: Int, val identifier: String) {
    PayId(0, "PayIdNotification"),
    VerifyId(1, "VerifyIdNotification")
}
