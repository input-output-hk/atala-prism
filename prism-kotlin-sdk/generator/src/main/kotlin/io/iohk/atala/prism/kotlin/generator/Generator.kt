package io.iohk.atala.prism.kotlin.generator

import pbandk.gen.ServiceGenerator
import java.nio.file.Paths

class Generator : ServiceGenerator {
    private fun generateKotlinCoroutineService(service: ServiceGenerator.Service): ServiceGenerator.Result {
        var interfaceMethods = emptyList<String>()
        var clientMethods = emptyList<String>()
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
        }
        return ServiceGenerator.Result(
            otherFilePath = Paths.get(service.filePath).resolveSibling(service.name + "Coroutine.kt").toString(),
            code =
            """
                    package ${service.file.kotlinPackageName}
                    
                    import io.iohk.atala.prism.kotlin.protos.PrismMetadata
                    
                    interface ${service.name}Coroutine {
                        ${interfaceMethods.joinToString("\n                        ")}
                        class Client(val client: io.iohk.atala.prism.kotlin.protos.GrpcClient) : ${service.name}Coroutine {
                            ${clientMethods.joinToString("")}
                        }
                    }
            """.trimIndent()
        )
    }

    private fun generateKotlinJsPromiseService(service: ServiceGenerator.Service): ServiceGenerator.Result {
        var clientMethodsJs = emptyList<String>()
        service.methods.forEach { method ->
            val reqType = service.kotlinTypeMappings[method.inputType!!]!!
            val respType = service.kotlinTypeMappings[method.outputType!!]!!
            clientMethodsJs += """
                        fun ${method.name}(req: $reqType): Promise<$respType> =
                            GlobalScope.promise { internalService.${method.name}(req) }
                        fun ${method.name}Auth(req: $reqType, metadata: PrismMetadata): Promise<$respType> =
                            GlobalScope.promise { internalService.${method.name}Auth(req, metadata) }
            """
        }
        return ServiceGenerator.Result(
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
                    class ${service.name}Promise(envoyOptions: GrpcEnvoyOptions) {
                        private val grpcClient = GrpcClient(
                            GrpcServerOptions(envoyOptions.protocol, envoyOptions.host, envoyOptions.port),
                            envoyOptions
                        )
                        val internalService = ${service.name}Coroutine.Client(grpcClient)
                        ${clientMethodsJs.joinToString("")}
                    }
            """.trimIndent()
        )
    }

    private fun generateJavaSyncPromiseService(
        service: ServiceGenerator.Service,
        javaPackageName: String,
        javaServiceRootDirectory: String
    ): ServiceGenerator.Result {
        var clientMethods = emptyList<String>()
        service.methods.forEach { method ->
            val reqType = service.kotlinTypeMappings[method.inputType!!]!!
            val respType = service.kotlinTypeMappings[method.outputType!!]!!
            clientMethods += """
                        fun ${method.name}(req: $reqType): $respType =
                            runBlocking { internalService.${method.name}(req) }
                        fun ${method.name}Auth(req: $reqType, metadata: PrismMetadata): $respType =
                            runBlocking { internalService.${method.name}Auth(req, metadata) }
            """
        }
        val filePath = Paths.get(javaServiceRootDirectory).resolveSibling("sync").resolve(service.name + "Sync.kt")
        return ServiceGenerator.Result(
            otherFilePath = "../../commonJvmAndroidMain/kotlin/$filePath",
            code =
            """
                    package $javaPackageName.sync
                    
                    import io.iohk.atala.prism.kotlin.protos.${service.name}Coroutine
                    import io.iohk.atala.prism.kotlin.protos.GrpcClient
                    import io.iohk.atala.prism.kotlin.protos.GrpcEnvoyOptions
                    import io.iohk.atala.prism.kotlin.protos.GrpcServerOptions
                    import io.iohk.atala.prism.kotlin.protos.PrismMetadata
                    import kotlinx.coroutines.runBlocking
                    
                    class ${service.name}Sync(envoyOptions: GrpcEnvoyOptions) {
                        private val grpcClient = GrpcClient(
                            GrpcServerOptions(envoyOptions.protocol, envoyOptions.host, envoyOptions.port),
                            envoyOptions
                        )
                        private val internalService = ${service.name}Coroutine.Client(grpcClient)
                        ${clientMethods.joinToString("")}
                    }
            """.trimIndent()
        )
    }

    private fun generateJavaAsyncPromiseService(
        service: ServiceGenerator.Service,
        javaPackageName: String,
        javaServiceRootDirectory: String
    ): ServiceGenerator.Result {
        var clientMethods = emptyList<String>()
        service.methods.forEach { method ->
            val reqType = service.kotlinTypeMappings[method.inputType!!]!!
            val respType = service.kotlinTypeMappings[method.outputType!!]!!
            clientMethods += """
                        fun ${method.name}(req: $reqType): CompletableFuture<$respType> =
                            GlobalScope.future { internalService.${method.name}(req) }
                        fun ${method.name}Auth(req: $reqType, metadata: PrismMetadata): CompletableFuture<$respType> =
                            GlobalScope.future { internalService.${method.name}Auth(req, metadata) }
            """
        }
        val filePath = Paths.get(javaServiceRootDirectory).resolveSibling("async").resolve(service.name + "Async.kt")
        return ServiceGenerator.Result(
            otherFilePath = "../../commonJvmAndroidMain/kotlin/$filePath",
            code =
            """
                    package $javaPackageName.async
                    
                    import io.iohk.atala.prism.kotlin.protos.${service.name}Coroutine
                    import io.iohk.atala.prism.kotlin.protos.GrpcClient
                    import io.iohk.atala.prism.kotlin.protos.GrpcEnvoyOptions
                    import io.iohk.atala.prism.kotlin.protos.GrpcServerOptions
                    import io.iohk.atala.prism.kotlin.protos.PrismMetadata
                    import kotlinx.coroutines.GlobalScope
                    import kotlinx.coroutines.future.future
                    import java.util.concurrent.CompletableFuture
                    
                    class ${service.name}Async(envoyOptions: GrpcEnvoyOptions) {
                        private val grpcClient = GrpcClient(
                            GrpcServerOptions(envoyOptions.protocol, envoyOptions.host, envoyOptions.port),
                            envoyOptions
                        )
                        private val internalService = ${service.name}Coroutine.Client(grpcClient)
                        ${clientMethods.joinToString("")}
                    }
            """.trimIndent()
        )
    }

    override fun generate(service: ServiceGenerator.Service): List<ServiceGenerator.Result> {
        service.debug { "Generating code for service ${service.name}" }
        // Renames `io.iohk.atala.prism.kotlin.protos` to `io.iohk.atala.prism.java.protos` to signify that this
        // API is especially useful for Java users.
        val javaPackageName = service.file.kotlinPackageName!!.replace("kotlin", "java")
        val javaServiceRootDirectory = service.filePath.replace("kotlin", "java")
        return listOf(
            generateKotlinCoroutineService(service),
            generateKotlinJsPromiseService(service),
            generateJavaSyncPromiseService(service, javaPackageName, javaServiceRootDirectory),
            generateJavaAsyncPromiseService(service, javaPackageName, javaServiceRootDirectory)
        )
    }
}
