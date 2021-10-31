package io.iohk.atala.prism.utils

import fs2.Stream
import fs2.concurrent.Queue
import io.grpc.stub.StreamObserver
import monix.eval.Task
import monix.execution.Scheduler

object GrpcStreamsUtils {

  /** Creates a Fs2Stream from grpc stream using bounded queue of specified size. When the queue is full,
    * queue.enqueue1() blocks until there is a free space in queue.
    */
  def createFs2Stream[A](registerStreamCallback: StreamObserver[A] => Unit, maxQueueSize: Int = 100)(implicit
      scheduler: Scheduler
  ): Stream[Task, A] = {
    Stream
      .eval(Queue.bounded[Task, Either[Throwable, A]](maxQueueSize))
      .flatMap { queue =>
        val responseObserver = new StreamObserver[A] {
          override def onNext(value: A): Unit = {
            queue.enqueue1(Right(value)).runSyncUnsafe()
          }

          override def onError(t: Throwable): Unit = {
            queue.enqueue1(Left(new Throwable(s"Error occurred when processing stream: $t"))).runSyncUnsafe()
          }

          override def onCompleted(): Unit = {
            queue.enqueue1(Left(new Throwable("Stream processing unexpectedly completed"))).runSyncUnsafe()
          }
        }

        registerStreamCallback(responseObserver)

        queue.dequeue
          .flatMap {
            case Right(value) => Stream.emit(value)
            case Left(error) => Stream.raiseError[Task](error)
          }
      }
  }

}
