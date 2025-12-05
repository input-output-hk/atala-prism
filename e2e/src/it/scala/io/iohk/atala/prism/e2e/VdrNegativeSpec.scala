package io.iohk.atala.prism.e2e

import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}

class VdrNegativeSpec extends VdrTestUtils {

  "VDR negative cases" should {
    "reject VDR create when signed with non-VDR key" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val badSignedCreate = signOperation(
        node_models.AtalaOperation().withCreateStorageEntry(
          node_models.CreateStorageEntryOperation()
            .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload")))
        ),
        keyId = "master", // wrong usage
        key = master.privateKey
      )

      val tryResp: Either[StatusRuntimeException, node_api.CreateVdrEntryResponse] =
        try {
          Right(client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(badSignedCreate))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      tryResp match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          val out = requireOutput(resp.output, "create VDR with bad key")
          val opId = operationIdOrFail(out)
          awaitRejected(opId) shouldBe common_models.OperationStatus.CONFIRMED_AND_REJECTED
      }
    }

    "reject VDR update with unknown previous hash" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      createDidWithVdrKey(master, vdr)

      val bogusPrev = ByteString.copyFromUtf8("deadbeef")
      val signedUpdate = signOperation(
        node_models.AtalaOperation().withUpdateStorageEntry(
          node_models.UpdateStorageEntryOperation()
            .withPreviousEventHash(bogusPrev)
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("x")))
        ),
        keyId = "vdr",
        key = vdr.privateKey
      )

      val ex = intercept[io.grpc.StatusRuntimeException] {
        client.updateVdrEntry(node_api.UpdateVdrEntryRequest(Some(signedUpdate)))
      }
      ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
    }

    "reject VDR deactivate with unknown previous hash" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      createDidWithVdrKey(master, vdr)

      val bogusPrev = ByteString.copyFromUtf8("cafebabe")
      val signedDeactivate = signOperation(
        node_models.AtalaOperation().withDeactivateStorageEntry(
          node_models.DeactivateStorageEntryOperation().withPreviousEventHash(bogusPrev)
        ),
        keyId = "vdr",
        key = vdr.privateKey
      )

      val ex = intercept[io.grpc.StatusRuntimeException] {
        client.deactivateVdrEntry(node_api.DeactivateVdrEntryRequest(Some(signedDeactivate)))
      }
      ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
    }

    "reject VDR create when DID has no VDR key" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val didSuffixHash = createDidWithoutVdr(master)

      val signedCreate = signOperation(
        node_models.AtalaOperation().withCreateStorageEntry(
          node_models.CreateStorageEntryOperation()
            .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload")))
        ),
        keyId = "master",
        key = master.privateKey
      )

      val respOrEx: Either[StatusRuntimeException, node_api.CreateVdrEntryResponse] =
        try {
          Right(client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(signedCreate))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      respOrEx match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          val out = requireOutput(resp.output, "create VDR without VDR key")
          val opId = operationIdOrFail(out)
          awaitRejected(opId) shouldBe common_models.OperationStatus.CONFIRMED_AND_REJECTED
      }
    }

    "reject VDR create when VDR key curve is not secp256k1" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val ex = intercept[StatusRuntimeException] {
        createDidWithCustomVdr(master, vdr.publicKey, curveOverride = "ed25519")
      }
      ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
    }

    "reject VDR create with invalid signature" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val createOp = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload")))
      )
      val badSigned = node_models.SignedAtalaOperation(
        signedWith = "vdr",
        signature = ByteString.EMPTY, // invalid signature
        operation = Some(createOp)
      )

      val respOrEx: Either[StatusRuntimeException, node_api.CreateVdrEntryResponse] =
        try {
          Right(client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(badSigned))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      respOrEx match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          val out = requireOutput(resp.output, "create VDR with invalid signature")
          val opId = operationIdOrFail(out)
          awaitRejected(opId) shouldBe common_models.OperationStatus.CONFIRMED_AND_REJECTED
      }
    }

    "reject VDR update when signed with non-VDR key" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)
      val (createEventHash, _) = createVdrEntry(didSuffixHash, vdr, "payload-1")

      val signedUpdateWithMaster = signOperation(
        node_models.AtalaOperation().withUpdateStorageEntry(
          node_models.UpdateStorageEntryOperation()
            .withPreviousEventHash(createEventHash)
            .withData(node_models.StorageData().withIpfsCid("cid-2"))
        ),
        keyId = "master",
        key = master.privateKey
      )

      val respOrEx: Either[StatusRuntimeException, node_api.UpdateVdrEntryResponse] =
        try {
          Right(client.updateVdrEntry(node_api.UpdateVdrEntryRequest(Some(signedUpdateWithMaster))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      respOrEx match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          val out = requireOutput(resp.output, "update VDR with master key")
          val opId = operationIdOrFail(out)
          awaitRejected(opId) shouldBe common_models.OperationStatus.CONFIRMED_AND_REJECTED
      }
    }

    "reject VDR deactivate when signed with non-VDR key" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)
      val (createEventHash, _) = createVdrEntry(didSuffixHash, vdr, "payload-1")
      val updateEventHash = updateVdrEntry(createEventHash, vdr, "cid-2")

      val signedDeactivateWithMaster = signOperation(
        node_models.AtalaOperation().withDeactivateStorageEntry(
          node_models.DeactivateStorageEntryOperation()
            .withPreviousEventHash(updateEventHash)
        ),
        keyId = "master",
        key = master.privateKey
      )

      val respOrEx: Either[StatusRuntimeException, node_api.DeactivateVdrEntryResponse] =
        try {
          Right(client.deactivateVdrEntry(node_api.DeactivateVdrEntryRequest(Some(signedDeactivateWithMaster))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      respOrEx match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          val out = requireOutput(resp.output, "deactivate VDR with master key")
          val opId = operationIdOrFail(out)
          awaitRejected(opId) shouldBe common_models.OperationStatus.CONFIRMED_AND_REJECTED
      }
    }

    "reject VDR create when using VDR key from another DID" taggedAs E2ETestTag in {
      val masterA = generateKeyPair()
      val vdrA = generateKeyPair()
      val masterB = generateKeyPair()
      val vdrB = generateKeyPair()

      val didA = createDidWithVdrKey(masterA, vdrA)
      createDidWithVdrKey(masterB, vdrB)

      val op = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didA.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("cross")))
      )
      val signed = signOperation(op, "vdr", vdrB.privateKey)

      val respOrEx: Either[StatusRuntimeException, node_api.CreateVdrEntryResponse] =
        try {
          Right(client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(signed))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      respOrEx match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          val out = requireOutput(resp.output, "cross-DID VDR key")
          val opId = operationIdOrFail(out)
          val status = awaitRejected(opId)
          status shouldBe common_models.OperationStatus.CONFIRMED_AND_REJECTED
      }
    }

    "reject VDR create after VDR key was removed from DID" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didHash = createDidWithVdrKey(master, vdr)

      removeVdrKeyFromDid(didHash, master)

      val op = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("after-removal")))
      )
      val signed = signOperation(op, "vdr", vdr.privateKey)

      val respOrEx: Either[StatusRuntimeException, node_api.CreateVdrEntryResponse] =
        try {
          Right(client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(signed))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      respOrEx match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          val out = requireOutput(resp.output, "create after VDR removal")
          val opId = operationIdOrFail(out)
          val status = awaitRejected(opId)
          status shouldBe common_models.OperationStatus.CONFIRMED_AND_REJECTED
      }
    }
  }
}
