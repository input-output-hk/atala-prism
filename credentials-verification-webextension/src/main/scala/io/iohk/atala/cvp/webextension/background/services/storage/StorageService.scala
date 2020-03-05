package io.iohk.atala.cvp.webextension.background.services.storage

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.UndefOr._

/**
  * Internal service available to the background context, which allows dealing with the storage local.
  */
private[background] class StorageService(implicit ec: ExecutionContext) {

  def store(key: String, value: js.Any): Future[Unit] = {
    chrome.storage.Storage.local.set(js.Dictionary(key -> value))
  }

  def load(key: String): Future[Option[js.Any]] = {
    chrome.storage.Storage.local
      .get(any2undefOrA(key))
      .map(_.get(key))
  }
}

private[background] object StorageService {}
