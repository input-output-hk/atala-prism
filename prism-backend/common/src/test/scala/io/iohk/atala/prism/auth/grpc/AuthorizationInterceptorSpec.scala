package io.iohk.atala.prism.auth.grpc

import com.typesafe.config.ConfigFactory
import io.grpc.ServerCall.Listener
import io.grpc._
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationContext.AuthTokenKeys
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito.{mock, times, verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AuthorizationInterceptorSpec extends AnyWordSpec with Matchers {
  "AuthorizationInterceptor" should {
    "intercept nothing when authEnabled is false" in {
      val globalConfig = ConfigFactory.load()
      val authorizationInterceptor = new AuthorizationInterceptor(globalConfig)
      val headers = new Metadata()
      val serverCall = mock(classOf[ServerCall[Int, Int]])
      val delegate = mock(classOf[Listener[Int]])
      val next = mock(classOf[ServerCallHandler[Int, Int]])
      when(next.startCall(any(classOf[ServerCall[Int, Int]]), mockEq(headers)))
        .thenReturn(delegate)

      val listener = authorizationInterceptor.interceptCall(serverCall, headers, next)
      listener.onReady()
      verify(next, times(1)).startCall(mockEq(serverCall), mockEq(headers))

    }

    "intercept allow when supplied prism auth token matches and authEnable is true" in {
      val configOveride = """api {
                            |    authTokens = [
                            |    "ShVvJ11AlVhLYv7OBO9sY9AOz8D5FoWo"
                            |    ]
                            |    authEnabled = true
                            |}""".stripMargin
      val globalConfig = ConfigFactory.parseString(configOveride).withFallback(ConfigFactory.load())

      val authorizationInterceptor = new AuthorizationInterceptor(globalConfig)
      val headers = new Metadata()
      headers.put(AuthTokenKeys.metadata, "ShVvJ11AlVhLYv7OBO9sY9AOz8D5FoWo")
      val serverCall = mock(classOf[ServerCall[Int, Int]])
      val delegate = mock(classOf[Listener[Int]])
      val next = mock(classOf[ServerCallHandler[Int, Int]])
      when(next.startCall(any(classOf[ServerCall[Int, Int]]), mockEq(headers)))
        .thenReturn(delegate)

      val listener = authorizationInterceptor.interceptCall(serverCall, headers, next)
      listener.onReady()
      verify(next, times(1)).startCall(mockEq(serverCall), mockEq(headers))

    }

    "intercept fail Unauthenticated when supplied prism auth token doesnt matched and authEnable is true" in {
      val configOveride = """api {
                            |    authTokens = [
                            |    "YYVvJ11AlVhLYv7OBO9sY9AOz8D5FoWo"
                            |    ]
                            |    authEnabled = true
                            |}""".stripMargin
      val globalConfig = ConfigFactory.parseString(configOveride).withFallback(ConfigFactory.load())

      val authorizationInterceptor = new AuthorizationInterceptor(globalConfig)
      val headers = new Metadata()
      headers.put(AuthTokenKeys.metadata, "ShVvJ11AlVhLYv7OBO9sY9AOz8D5FoWo")
      val serverCall = mock(classOf[ServerCall[Int, Int]])
      val delegate = mock(classOf[Listener[Int]])
      val next = mock(classOf[ServerCallHandler[Int, Int]])
      when(next.startCall(any(classOf[ServerCall[Int, Int]]), mockEq(headers)))
        .thenReturn(delegate)

      val listener = authorizationInterceptor.interceptCall(serverCall, headers, next)
      listener.onReady()
      listener.onMessage(1)
      listener.onComplete()
      listener.onCancel()
      verify(next, times(0)).startCall(mockEq(serverCall), mockEq(headers))

    }
  }
}
