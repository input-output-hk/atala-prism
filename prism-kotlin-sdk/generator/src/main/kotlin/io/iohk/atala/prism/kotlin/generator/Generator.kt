package io.iohk.atala.prism.kotlin.generator

import pbandk.gen.ServiceGenerator
import java.nio.file.Paths

class Generator : ServiceGenerator {
    override fun generate(service: ServiceGenerator.Service): List<ServiceGenerator.Result> {
        service.debug { "Generating code for service ${service.name}" }
        var interfaceMethods = emptyList<String>()
        var clientMethods = emptyList<String>()
        service.methods.forEach { method ->
            val reqType = service.kotlinTypeMappings[method.inputType!!]!!
            val respType = service.kotlinTypeMappings[method.outputType!!]!!
            val serviceNameLit = "\"${service.file.packageName}.${service.name}\""
            val methodNameLit = "\"${method.name}\""
            interfaceMethods += "suspend fun ${method.name}(req: $reqType): $respType"
            clientMethods += """
                        override suspend fun ${method.name}(req: $reqType): $respType {
                            return client.call(req, $reqType.Companion, $respType.Companion, $serviceNameLit, $methodNameLit)
                        }
            """
        }
        return listOf(
            ServiceGenerator.Result(
                otherFilePath = Paths.get(service.filePath).resolveSibling(service.name + ".kt").toString(),
                code =
                    """
                    package ${service.file.kotlinPackageName}
                    
                    interface ${service.name} {
                        ${interfaceMethods.joinToString("\n                    ")}
                        class Client(val client: io.iohk.atala.prism.kotlin.protos.GrpcClient) : ${service.name} {
                            ${clientMethods.joinToString("")}
                        }
                    }
                    """.trimIndent()
            )
        )
    }
}
