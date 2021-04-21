package io.iohk.atala.prism.console

import cats.instances.list._
import cats.instances.try_._
import cats.syntax.traverse._
import io.circe.Json
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.console.models.actions._
import io.iohk.atala.prism.console.models.{
  Contact,
  CredentialExternalId,
  GenericCredential,
  IssuerGroup,
  RevokePublishedCredential
}
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.protos.console_api

import java.util.UUID
import scala.util.{Failure, Success, Try}

package object grpc {
  implicit val revokePublishedCredentialConverter
      : ProtoConverter[console_api.RevokePublishedCredentialRequest, RevokePublishedCredential] = { request =>
    {
      for {
        credentialId <- GenericCredential.Id.from(request.credentialId)
        operation <- Try {
          request.revokeCredentialsOperation
            .getOrElse(throw new RuntimeException("Missing revokeCredentialsOperation"))
        }
        _ <- Try {
          if (operation.operation.exists(_.operation.isRevokeCredentials)) ()
          else throw new RuntimeException("Invalid revokeCredentialsOperation, it is a different operation")
        }

        _ <- Try {
          val credentialHashes = operation.operation
            .flatMap(_.operation.revokeCredentials)
            .map(_.credentialsToRevoke)
            .getOrElse(Seq.empty)

          if (credentialHashes.size == 1) {
            ()
          } else {
            val msg = if (credentialHashes.isEmpty) {
              s"Invalid revokeCredentialsOperation, a single credential is expected but the whole batch was found"
            } else {
              s"Invalid revokeCredentialsOperation, a single credential is expected but ${credentialHashes.size} credentials found"
            }
            throw new RuntimeException(msg)
          }
        }
      } yield RevokePublishedCredential(credentialId, operation)
    }
  }

  implicit val createContactRequestConverter
      : ProtoConverter[console_api.CreateContactRequest, CreateContactsRequest] = { in =>
    for {
      externalId <-
        if (in.externalId.trim.isEmpty) Failure(new RuntimeException("externalId cannot be empty"))
        else Success(Contact.ExternalId(in.externalId.trim))
      json <- {
        val jsonData = Option(in.jsonData).filter(_.nonEmpty).getOrElse("{}")
        io.circe.parser
          .parse(jsonData)
          .fold(
            _ => Failure(new RuntimeException("Invalid jsonData: it must be a JSON string")),
            json => Success(json)
          )
      }
      maybeGroupName = if (in.groupName.trim.isEmpty) None else Some(IssuerGroup.Name(in.groupName.trim))
    } yield CreateContactsRequest(externalId, json, maybeGroupName)
  }

  implicit val getContactsRequestConverter: ProtoConverter[console_api.GetContactsRequest, GetContactsRequest] = { in =>
    val maybeGroupName = Option(in.groupName.trim).filter(_.nonEmpty).map(IssuerGroup.Name.apply)
    Success(GetContactsRequest(Contact.Id.from(in.lastSeenContactId).toOption, maybeGroupName))
  }

  implicit val getContactRequestConverter: ProtoConverter[console_api.GetContactRequest, GetContactRequest] = { in =>
    Contact.Id.from(in.contactId).map(GetContactRequest)
  }

  implicit val generateConnectionTokenForContactRequestConverter: ProtoConverter[
    console_api.GenerateConnectionTokenForContactRequest,
    GenerateConnectionTokenForContactRequest
  ] = { in =>
    Contact.Id.from(in.contactId).map(GenerateConnectionTokenForContactRequest)
  }

  implicit val storeCredentialRequestConverter: ProtoConverter[
    console_api.StoreCredentialRequest,
    StoreCredentialRequest
  ] = { in =>
    for {
      externalId <- Try { CredentialExternalId(in.credentialExternalId) }
      connectionId <- ConnectionId.from(in.connectionId)
      merkleProof <-
        MerkleInclusionProof
          .decode(in.batchInclusionProof)
          .fold[Try[MerkleInclusionProof]](
            Failure(new RuntimeException(s"Failed to decode merkle proof: ${in.batchInclusionProof}"))
          )(proof => Success(proof))
    } yield StoreCredentialRequest(externalId, connectionId, merkleProof)
  }

  implicit val getStoredCredentialsForRequestConverter: ProtoConverter[
    console_api.GetStoredCredentialsForRequest,
    GetStoredCredentialsForRequest
  ] = { in =>
    val contactId = if (in.individualId.nonEmpty) Contact.Id.from(in.individualId).map(Some(_)) else Success(None)
    contactId.map(GetStoredCredentialsForRequest)
  }

  implicit val createGroupRequestConverter: ProtoConverter[
    console_api.CreateGroupRequest,
    CreateGroupRequest
  ] = { in =>
    Success(CreateGroupRequest(IssuerGroup.Name(in.name)))
  }

  implicit val getGroupsRequestConverter: ProtoConverter[
    console_api.GetGroupsRequest,
    GetGroupsRequest
  ] = { in =>
    val contactId =
      if (in.contactId.nonEmpty)
        Contact.Id.from(in.contactId).map(Option.apply)
      else Try(None)
    contactId.map(GetGroupsRequest.apply)
  }

  implicit val updateGroupRequestConverter: ProtoConverter[console_api.UpdateGroupRequest, UpdateGroupRequest] = { in =>
    for {
      maybeGroupId <- IssuerGroup.Id.from(in.groupId)
      contactsToAdd <- in.contactIdsToAdd.toList.traverse(id => Contact.Id.from(id))
      contactsToRemove <- in.contactIdsToRemove.toList.traverse(id => Contact.Id.from(id))
    } yield UpdateGroupRequest(maybeGroupId, contactsToAdd, contactsToRemove)
  }

  implicit val storePublishedCredentialRequestConverter
      : ProtoConverter[console_api.StorePublishedCredentialRequest, StorePublishedCredentialRequest] = { in =>
    for {
      id <- Try(GenericCredential.Id(UUID.fromString(in.consoleCredentialId)))
      encodedSignedCredential <-
        if (in.encodedSignedCredential.nonEmpty) Success(in.encodedSignedCredential)
        else Failure(new IllegalArgumentException("Empty encoded credential"))
      batchId <-
        CredentialBatchId
          .fromString(in.batchId)
          .fold[Try[CredentialBatchId]](Failure(new IllegalArgumentException("Invalid batch id")))(Success.apply)
      proof <-
        MerkleInclusionProof
          .decode(in.encodedInclusionProof)
          .fold[Try[MerkleInclusionProof]](Failure(new IllegalArgumentException("Empty inclusion proof")))(
            Success.apply
          )
    } yield StorePublishedCredentialRequest(encodedSignedCredential, id, batchId, proof)
  }

  implicit val publishBatchRequestConverter: ProtoConverter[console_api.PublishBatchRequest, PublishBatchRequest] =
    _.issueCredentialBatchOperation
      .fold[Try[PublishBatchRequest]](Failure(new RuntimeException("Missing IssueCredentialBatch operation")))(op =>
        Success(PublishBatchRequest(op))
      )

  implicit val getBlockchainDataRequestConverter
      : ProtoConverter[console_api.GetBlockchainDataRequest, GetBlockchainDataRequest] = { in =>
    // NOTE: Until we implement proper batching in the wallet, we will assume that the credential
    //       was published with a batch that contained the hash of the encodedSignedCredential
    //       as MerkleRoot in the IssueCredentialBatch operation. This allows us to compute the
    //       credential batch id without requesting the merkle root to the client.
    val result = for {
      credential <- JsonBasedCredential.fromString(in.encodedSignedCredential)
      issuerDid <- credential.content.issuerDid
      batchId = CredentialBatchId.fromBatchData(issuerDid.suffix, MerkleRoot(credential.hash))
    } yield GetBlockchainDataRequest(batchId)
    result.toTry
  }

  implicit val shareCredentialRequestConverter
      : ProtoConverter[console_api.ShareCredentialRequest, ShareCredentialRequest] = { in =>
    GenericCredential.Id.from(in.cmanagerCredentialId).map(ShareCredentialRequest)
  }

  implicit val getContactCredentialRequestConverter
      : ProtoConverter[console_api.GetContactCredentialsRequest, GetContactCredentialsRequest] = { in =>
    Contact.Id.from(in.contactId).map(GetContactCredentialsRequest)
  }

  implicit val getGenericCredentialsRequestConverter
      : ProtoConverter[console_api.GetGenericCredentialsRequest, GetGenericCredentialsRequest] = { in =>
    Success(GetGenericCredentialsRequest(in.limit, in.offset))
  }

  implicit val createGenericCredentialRequestConverter
      : ProtoConverter[console_api.CreateGenericCredentialRequest, CreateGenericCredentialRequest] = { in =>
    for {
      json <-
        io.circe.parser
          .parse(in.credentialData)
          .fold[Try[Json]](_ => Failure(new RuntimeException("Invalid json")), Success.apply)
      externalId = Option(in.externalId)
        .filter(_.nonEmpty)
        .map(Contact.ExternalId.apply)
      contactId = Contact.Id.from(in.contactId).toOption
    } yield CreateGenericCredentialRequest(contactId, externalId, json)
  }

}
