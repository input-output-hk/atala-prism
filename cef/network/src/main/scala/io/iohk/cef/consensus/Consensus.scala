package io.iohk.cef.consensus

import io.iohk.cef.network.Node

/**
  * Abstraction for a consensus protocol implementation.
  *
  */
trait Consensus {

  type Protocol
  type FullConsensusConfig[T]
  type VMImpl
  type Validators
  type BlockGenerator
  type BlockPreparator

  /**
    * The type of configuration
    * to this consensus protocol implementation.
    */
  type Config <: AnyRef /*Product*/

  def protocol: Protocol

  def config: FullConsensusConfig[Config]

  /**
    * This is the VM used while preparing and generating blocks.
    */
  def vm: VMImpl

  /**
    * Provides the set of validators specific to this consensus protocol.
    */
  def validators: Validators

  /**
    * This is used by the
    */
  def blockPreparator: BlockPreparator

  /**
    * Returns the BlockGenerator
    * this consensus protocol uses.
    */
  def blockGenerator: BlockGenerator

  /**
    * Starts the consensus protocol on the current `node`.
    */
  def startProtocol(node: Node): Unit

  /**
    * Stops the consensus protocol on the current node.
    * This is called internally when the node terminates.
    */
  def stopProtocol(): Unit
}
