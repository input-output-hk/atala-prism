package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.neo.data.local.KycLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource

class KycRepository(
    private val kycLocalDataSource: KycLocalDataSourceInterface,
    private val remoteDataSource: ConnectorRemoteDataSource,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) : BaseRepository(sessionLocalDataSource, preferencesLocalDataSource) {

    // Due to the complexity of initiating the flow of Acuant, [KycInitializationHelper] was created
    private val kycInitializationHelper = KycInitializationHelper(kycLocalDataSource, remoteDataSource, sessionLocalDataSource)

    val kycInitializationStatus = kycInitializationHelper.status

    fun initializeAcuantProcess(): LiveData<KycInitializationHelper.KycInitializationResult?> = kycInitializationHelper.initializeAcuantProcess()
}
