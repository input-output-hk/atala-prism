package io.iohk.atala.prism.kotlin.generator

import pbandk.gen.ServiceGenerator
import java.nio.file.Paths

class Generator : ServiceGenerator {
    override fun generate(service: ServiceGenerator.Service): List<ServiceGenerator.Result> {
        service.debug { "Generating code for service ${service.name}" }
        var interfaceMethods = emptyList<String>()
        var clientMethods = emptyList<String>()
        var clientMethodsJs = emptyList<String>()
        service.methods.forEach { method ->
            val reqType = service.kotlinTypeMappings[method.inputType!!]!!
            val respType = service.kotlinTypeMappings[method.outputType!!]!!
            val serviceNameLit = "\"${service.file.packageName}.${service.name}\""
            val methodNameLit = "\"${method.name}\""
            interfaceMethods += "suspend fun ${method.name}(req: $reqType): $respType"
            interfaceMethods += "suspend fun ${method.name}Auth(req: $reqType, metadata: PrismMetadata): $respType"
            clientMethods += """
                            override suspend fun ${method.name}(req: $reqType): $respType {
                                return client.call(req, $reqType.Companion, $respType.Companion, $serviceNameLit, $methodNameLit)
                            }
                            override suspend fun ${method.name}Auth(req: $reqType, metadata: PrismMetadata): $respType {
                                return client.callAuth(req, $reqType.Companion, $respType.Companion, $serviceNameLit, $methodNameLit, metadata)
                            }
            """
            clientMethodsJs += """
                        fun ${method.name}(req: $reqType): Promise<$respType> =
                            GlobalScope.promise { internalService.${method.name}(req) }
                        fun ${method.name}Auth(req: $reqType, metadata: PrismMetadata): Promise<$respType> =
                            GlobalScope.promise { internalService.${method.name}Auth(req, metadata) }
            """
        }
        return listOf(
            ServiceGenerator.Result(
                otherFilePath = Paths.get(service.filePath).resolveSibling(service.name + ".kt").toString(),
                code =
                    """
                    package ${service.file.kotlinPackageName}
                    
                    import io.iohk.atala.prism.kotlin.protos.PrismMetadata
                    
                    interface ${service.name} {
                        ${interfaceMethods.joinToString("\n                        ")}
                        class Client(val client: io.iohk.atala.prism.kotlin.protos.GrpcClient) : ${service.name} {
                            ${clientMethods.joinToString("")}
                        }
                    }
                    """.trimIndent()
            ),
            ServiceGenerator.Result(
                otherFilePath = "../../jsMain/kotlin/" + Paths.get(service.filePath).resolveSibling(service.name + "JS.kt").toString(),
                code =
                    """
                    package ${service.file.kotlinPackageName}
                    
                    import io.iohk.atala.prism.kotlin.protos.GrpcClient
                    import io.iohk.atala.prism.kotlin.protos.GrpcEnvoyOptions
                    import io.iohk.atala.prism.kotlin.protos.PrismMetadata
                    import kotlinx.coroutines.GlobalScope
                    import kotlinx.coroutines.promise
                    import kotlin.js.Promise
                    import kotlin.js.JsExport
                    import kotlin.js.JsName
                    
                    @JsExport
                    class ${service.name}JS(envoyOptions: GrpcEnvoyOptions) {
                        private val grpcClient = GrpcClient(
                            GrpcServerOptions(envoyOptions.protocol, envoyOptions.host, envoyOptions.port),
                            envoyOptions
                        )
                        private val internalService = ${service.name}.Client(grpcClient)
                        ${clientMethodsJs.joinToString("")}
                    }
                    """.trimIndent()
            )
        )
    }
}
