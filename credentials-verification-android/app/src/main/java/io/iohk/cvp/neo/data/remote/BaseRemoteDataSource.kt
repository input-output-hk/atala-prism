package io.iohk.cvp.neo.data.remote

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.iohk.cvp.BuildConfig
import io.iohk.cvp.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.cvp.neo.model.BackendConfig

open class BaseRemoteDataSource(private val sessionLocalDataSource: SessionLocalDataSourceInterface) {

    companion object {
        const val QUERY_LENGTH_LIMIT = 100
    }

    private var mainChannel: ManagedChannel? = null
    private var currentBackendConfig = BackendConfig(BuildConfig.API_BASE_URL, BuildConfig.API_PORT)

    @Synchronized
    protected fun getMainChannel(): ManagedChannel {
        val expectedConfiguration = sessionLocalDataSource.getCustomBackendConfig()
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