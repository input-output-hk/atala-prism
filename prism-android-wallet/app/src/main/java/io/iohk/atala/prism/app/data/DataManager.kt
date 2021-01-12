package io.iohk.atala.prism.app.data

import io.iohk.atala.prism.app.data.local.db.DbHelper
import io.iohk.atala.prism.app.data.local.preferences.PreferencesHelper
import io.iohk.atala.prism.app.data.local.remote.ApiHelper

interface DataManager : DbHelper, ApiHelper, PreferencesHelper