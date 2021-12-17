package io.iohk.atala.prism

import io.iohk.atala.prism.api.node.NodeAuthApi
import io.iohk.atala.prism.api.node.NodeAuthApiImpl
import io.iohk.atala.prism.api.node.NodePublicApi
import io.iohk.atala.prism.api.node.NodePublicApiImpl
import io.iohk.atala.prism.common.PrismSdkInternal
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.protos.GrpcClient
import io.iohk.atala.prism.protos.GrpcOptions
import io.iohk.atala.prism.protos.NodeServiceCoroutine
import kotlinx.cli.*

data class HostPort(var host: String, val port: Int)

@OptIn(ExperimentalCli::class)
class CreateDidCommand(val createDid: () -> Unit) :
    Subcommand("create-did", "Created DID and wait until the operation gets confirmed") {
    override fun execute() = createDid()
}

@OptIn(ExperimentalCli::class)
class HealthCheckCommand(val healthCheck: () -> Unit) : Subcommand("health-check", "Node health check") {
    override fun execute() = healthCheck()
}

@OptIn(ExperimentalCli::class)
class GetBuildInfoCommand(val getBuildInfo: () -> Unit) : Subcommand("build-info", "Get Node build info") {
    override fun execute() = getBuildInfo()
}

@OptIn(ExperimentalCli::class)
class ResolveDIDCommand(val resolveDid: (PrismDid) -> Unit) : Subcommand("resolve-did", "Resolve DID document by DID") {
    val rawDid by option(ArgType.String, "did", "d", "Short form DID")
    lateinit var did: PrismDid

    override fun execute() {
        did = PrismDid.fromString(rawDid!!)
        resolveDid(did)
    }
}

@OptIn(PrismSdkInternal::class)
interface CommandsHandlers {
    fun healthCheck(nodePublicApi: NodePublicApi)
    fun getBuildInfo(asyncClient: NodeServiceCoroutine)
    fun createDid(asyncClient: NodeServiceCoroutine, nodeAuthApi: NodeAuthApi)
    fun resolveDid(nodePublicApi: NodePublicApi, did: PrismDid)
}

@OptIn(ExperimentalCli::class, PrismSdkInternal::class)
fun parseArgs(args: Array<String>, handlers: CommandsHandlers) {
    val parser = ArgParser("node-cli")
    val host by parser.option(ArgType.String, "host", "H", "Node host").default("master.atalaprism.io")
    val port by parser.option(ArgType.Int, "port", "p", "Node port").default(50053)

    val createDid = CreateDidCommand {
        val nodeAuthApi = NodeAuthApiImpl(GrpcOptions("https", host, port))
        val asyncClient = NodeServiceCoroutine.Client(GrpcClient(GrpcOptions("https", host, port)))
        handlers.createDid(asyncClient, nodeAuthApi)
    }

    val healthCheck = HealthCheckCommand {
        val nodePublicApi = NodePublicApiImpl(GrpcOptions("https", host, port))
        handlers.healthCheck(nodePublicApi)
    }

    val buildInfo = GetBuildInfoCommand {
        handlers.getBuildInfo(NodeServiceCoroutine.Client(GrpcClient(GrpcOptions("https", host, port))))
    }

    val resolveDIDCommand = ResolveDIDCommand {
        val nodePublicApi = NodePublicApiImpl(GrpcOptions("https", host, port))
        handlers.resolveDid(nodePublicApi, it)
    }

    parser.subcommands(createDid, healthCheck, buildInfo, resolveDIDCommand)

    parser.parse(args)
}
