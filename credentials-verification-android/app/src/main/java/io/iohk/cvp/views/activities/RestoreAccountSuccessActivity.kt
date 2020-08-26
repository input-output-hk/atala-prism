package io.iohk.cvp.views.activities

import android.content.Intent
import android.os.Bundle
import io.iohk.cvp.R
import io.iohk.cvp.views.Navigator
import kotlinx.android.synthetic.main.activity_restore_account_success.*
import javax.inject.Inject

class RestoreAccountSuccessActivity : CvpActivity<Nothing>() {

    @Inject
    lateinit var nav: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_account_success)
        init()
    }

    private fun init() {
        next_btn.setOnClickListener {
            val flags: MutableList<Int> = ArrayList()
            flags.add(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            flags.add(Intent.FLAG_ACTIVITY_NEW_TASK)
            navigator.showConnections(this, flags)
        }
    }


    override fun getView(): Int {
        return R.layout.activity_restore_account_success
    }

    override fun getTitleValue(): Int {
        return R.string.empty_title
    }

    override fun getViewModel(): Nothing? {
        return null
    }

    override fun getNavigator(): Navigator {
        return nav
    }
}