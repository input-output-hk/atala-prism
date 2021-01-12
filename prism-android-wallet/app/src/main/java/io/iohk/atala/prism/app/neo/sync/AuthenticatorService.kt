package io.iohk.atala.prism.app.neo.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class AuthenticatorService : Service() {

    companion object {
        const val ACCOUNT_TYPE = "io.iohk.cvp"
        const val ACCOUNT_NAME = "Atala Prism"
        const val ACCOUNT_AUTHORITY = "io.iohk.atala.prism.app.neo.sync"

        fun buildGenericAccountForSync(context: Context): Account {
            val newAccount = Account(ACCOUNT_NAME, ACCOUNT_TYPE)
            val accountManager = context.getSystemService(ACCOUNT_SERVICE) as AccountManager
            accountManager.addAccountExplicitly(newAccount, null, null)
            return newAccount
        }
    }

    private lateinit var authenticator: Authenticator

    override fun onCreate() {
        authenticator = Authenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? = authenticator.iBinder
}