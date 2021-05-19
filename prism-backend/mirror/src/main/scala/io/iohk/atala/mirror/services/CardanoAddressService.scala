package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.models.{CardanoAddress, CardanoAddressKey}
import io.iohk.atala.mirror.services.CardanoAddressService.CardanoAddressServiceError

import sys.process._
import java.io.{ByteArrayOutputStream, PrintWriter}

class CardanoAddressService(val binaryPath: String = "target/mirror-binaries/cardano-address") {

  def generateAddressKey(
      extendedPublicKey: String,
      path: String
  ): Either[CardanoAddressServiceError, CardanoAddressKey] = {
    runCommand(("echo" :: extendedPublicKey :: Nil) #| (binaryPath :: "key" :: "child" :: path :: Nil))
      .map(_.trim)
      .map(CardanoAddressKey)
  }

  def generateWalletAddress(
      extendedPublicKey: String,
      index: Int,
      network: String
  ): Either[CardanoAddressServiceError, CardanoAddress] = {
    for {
      spendingKey <- generateAddressKey(extendedPublicKey, s"0/$index")
      stakingKey <- generateAddressKey(extendedPublicKey, s"2/$index")
      address <- runCommand {
        ("echo" :: spendingKey.value :: Nil) #|
          (binaryPath :: "address" :: "payment" :: "--network-tag" :: network :: Nil) #|
          (binaryPath :: "address" :: "delegation" :: stakingKey.value :: Nil)
      }.map(_.trim)
    } yield CardanoAddress(address)
  }

  private def runCommand(processBuilder: ProcessBuilder): Either[CardanoAddressServiceError, String] = {
    val stdoutStream = new ByteArrayOutputStream
    val stderrStream = new ByteArrayOutputStream
    val stdoutWriter = new PrintWriter(stdoutStream)
    val stderrWriter = new PrintWriter(stderrStream)
    val exitValue = processBuilder.!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
    stdoutWriter.close()
    stderrWriter.close()
    if (exitValue == 0)
      Right(stdoutStream.toString)
    else
      Left(CardanoAddressServiceError(stderrStream.toString))
  }

}

object CardanoAddressService {
  case class CardanoAddressServiceError(consoleErrorOutput: String) extends AnyVal
}
