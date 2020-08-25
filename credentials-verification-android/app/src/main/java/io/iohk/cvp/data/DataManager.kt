package io.iohk.cvp.data

import io.iohk.cvp.data.local.db.DbHelper
import io.iohk.cvp.data.local.preferences.PreferencesHelper
import io.iohk.cvp.data.local.remote.ApiHelper

interface DataManager : DbHelper, ApiHelper, PreferencesHelper