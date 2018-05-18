package io.iohk.cef.utils

import java.time.{Clock, Instant}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/**
  * A map that has a limited size and the concept of expiration.
  * Once the max size has been reached, new pairs can be added if any of the existing pairs is expired.
  * @param maxSize
  * @param expiration
  * @param clock
  * @tparam K
  * @tparam V
  */
case class FiniteSizedMap[K,V](maxSize: Int, expiration: FiniteDuration, clock: Clock) {

  case class Expiring(value: V, expirationTimestamp: Instant) {
    def hasExpired = expirationTimestamp.isBefore(clock.instant())
  }

  private val map: mutable.LinkedHashMap[K,Expiring] = mutable.LinkedHashMap.empty

  def get(key: K): Option[V] = map.get(key).map(_.value)

  def put(key: K, value: V): Option[V] = {
    if(maxSize > map.size) {
      map.put(key, Expiring(value, clock.instant().plusMillis(expiration.toMillis))).map(_.value)
    } else if (map.head._2.hasExpired) {
      map -= map.head._1
      put(key, value)
    } else None
  }

  def +=(keyValue: (K, V)) = {
    put(keyValue._1, keyValue._2)
    this
  }

  def -=(key: K) = map -= key

  def dropExpired: Seq[(K,V)] = map.dropWhile(_._2.hasExpired).map(pair => (pair._1, pair._2.value)).toSeq

  def values = map.values.map(_.value)
}
