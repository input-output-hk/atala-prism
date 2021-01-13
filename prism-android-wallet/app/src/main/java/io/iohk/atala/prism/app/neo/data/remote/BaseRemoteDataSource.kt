package io.iohk.atala.prism.app.neo.data.remote

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.cvp.BuildConfig
import io.iohk.atala.prism.app.neo.model.BackendConfig

open class BaseRemoteDataSource(private val preferencesLocalDataSource: PreferencesLocalDataSourceInterface) {

    companion object {
        const val QUERY_LENGTH_LIMIT = 100
    }

    private var mainChannel: ManagedChannel? = null
    private var currentBackendConfig = BackendConfig(BuildConfig.API_BASE_URL, BuildConfig.API_PORT)

    @Synchronized
    protected fun getMainChannel(): ManagedChannel {
        val expectedConfiguration = preferencesLocalDataSource.getCustomBackendConfig()
                ?: BackendConfig(BuildConfig.API_BASE_URL, BuildConfig.API_PORT)
        if (mainChannel == null || expectedConfiguration != currentBackendConfig) {
            mainChannel = ManagedChannelBuilder
                    .forAddress(expectedConfiguration.url, expectedConfiguration.port)
                    .usePlaintext()
                    .build()
            currentBackendConfig = expectedConfiguration
        }
        return mainChannel!!
    }
}