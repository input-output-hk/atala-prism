package io.iohk.dids

import java.net.URI
import java.util.UUID

import scala.util.matching.Regex

trait DIDDriver {
  def get(uri: URI): Option[Document]
}

trait SimpleDIDManagement {
  def put(uri: URI, value: Document): Unit
  def update(uri: URI, value: Document): Unit
}

object InMemoryDIDDriver {
  val METHOD_NAME = "memory"

  val SCHEME_SPECIFIC_PART_RE: Regex = {
    val uuidReStr = raw"[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}"
    s"$METHOD_NAME:($uuidReStr)".r
  }

  val GENERIC_SCHEME_SPECIFIC_PART_RE = s"$METHOD_NAME:.*".r
}

class InMemoryDIDDriver extends DIDDriver with SimpleDIDManagement {
  import InMemoryDIDDriver._
  private var store: Map[UUID, Document] = Map.empty

  private def uriToUuid(uri: URI): UUID = {
    require(uri.getScheme == "did")

    val uuidString = uri.getSchemeSpecificPart match {
      case SCHEME_SPECIFIC_PART_RE(uuidString) => uuidString
      case GENERIC_SCHEME_SPECIFIC_PART_RE() =>
        throw new MalformedDIDException("Malformed memory DID method URI: proper format is memory:[UUID]")
      case _ => throw new IllegalArgumentException("Provided DID is not a memory one")
    }

    UUID.fromString(uuidString)
  }

  override def get(uri: URI): Option[Document] = {
    val uuid = uriToUuid(uri)
    store.get(uuid)
  }

  override def put(uri: URI, value: Document): Unit = {
    val uuid = uriToUuid(uri)
    this.synchronized {
      if (store.contains(uuid)) {
        throw new UpdateException("DID already exists")
      }
      store += uuid -> value
    }
  }

  private def changeAllowed(identifier: UUID, newValue: Document, oldValue: Document): Boolean = true

  override def update(uri: URI, value: Document): Unit = {
    val uuid = uriToUuid(uri)
    def currentValue = store.getOrElse(uuid, throw new UpdateException("DID does not exist"))
    if (changeAllowed(uuid, value, currentValue)) {
      this.synchronized {
        if (!store.get(uuid).contains(currentValue)) {
          throw new UpdateException("DID Document modified during processing update request")
        }
        store += uuid -> value
      }
    } else {
      throw new UpdateException("Not authorized to modify DID")
    }
  }
}
