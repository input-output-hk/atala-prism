package io.iohk.cef.ledger

/**
  * Represents the ledger state. It is encoded as a map because the ledger state is expected to be partitioned in
  * order to fit in memory. Each entry (String, S) of the map is a partition identified by an id of type String and
  * a value of type S.
  * The partitioning is contextual. It will change from ledger to ledger. A state is well partitioned iff:
  * (1) for any transaction, the set of partitions P it depends on can be clearly identified.
  * (2) for any transaction, P fits in memory.
  *
  * Let S be the ledger state and Q be a set of partitions of S. Then:
  *   A transaction t is dependent on Q iff t(S - Q) != t(S)
  * Meaning that the application of t in S without Q results in a different scenario than applying t on S.
  * @param map
  * @tparam S
  */
case class Partitioned[S](map: Map[String, S]) {
  def get(key: String): Option[S] = map.get(key)
  def contains(key: String): Boolean = map.contains(key)
  def put(key: String, value: S): Partitioned[S] = Partitioned(map + ((key, value)))
  def remove(key: String): Partitioned[S] = Partitioned(map - key)
  def keys: Set[String] = map.keySet
}
