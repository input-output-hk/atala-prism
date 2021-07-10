package io.iohk.atala.prism.app.neo.data.remote

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.model.BackendConfig
import io.iohk.cvp.BuildConfig

open class BaseRemoteDataSource(
    private val preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) {

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

    private var mirrorChannel: ManagedChannel? = null
    private var currentMirrorBackendConfig = BackendConfig(BuildConfig.API_BASE_URL, BuildConfig.MIRROR_SERVICE_PORT)

    @Synchronized
    protected fun getMirrorServiceChannel(): ManagedChannel {
        /** The base url should be exactly the same as the main channel, only the port should be used: [BuildConfig.KYC_BRIDGE_PORT] */
        val customConfig = preferencesLocalDataSource.getCustomBackendConfig()
            ?.let { BackendConfig(it.url, BuildConfig.MIRROR_SERVICE_PORT) }
        val expectedConfiguration =
            customConfig ?: BackendConfig(BuildConfig.API_BASE_URL, BuildConfig.MIRROR_SERVICE_PORT)
        if (mirrorChannel == null || expectedConfiguration != currentMirrorBackendConfig) {
            mirrorChannel = ManagedChannelBuilder
                .forAddress(expectedConfiguration.url, expectedConfiguration.port)
                .usePlaintext()
                .build()
            currentMirrorBackendConfig = expectedConfiguration
        }
        return mirrorChannel!!
    }

    val kycBridgeChannel: ManagedChannel by lazy {
        ManagedChannelBuilder
            .forAddress(BuildConfig.KYC_BRIDGE_BASE_URL, BuildConfig.KYC_BRIDGE_PORT)
            .usePlaintext()
            .build()
    }
}
