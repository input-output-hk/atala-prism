package io.iohk.atala.prism.task.lease.system

import doobie.{FC, Meta}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.update.Update
import io.iohk.atala.prism.utils.DoobieImplicits.jsonMeta
import cats.implicits._

class ProcessingTaskDao[S <: ProcessingTaskState](stringToProcessingTaskState: String => Option[S]) {

  implicit val processingTaskStateMeta: Meta[S] =
    Meta[String].imap[S](name =>
      stringToProcessingTaskState(name)
        .getOrElse(
          throw new NotImplementedError(
            s"Instance cannot process task with state: $name. " +
              s"Most probably instance has not been updated yet and other updated instance has created task with this state."
          )
        )
    )(_.entryName)

  def insert(processingTask: ProcessingTask[S]): ConnectionIO[Int] =
    insertMany.toUpdate0(processingTask).run

  def insertMany: Update[ProcessingTask[S]] =
    Update[ProcessingTask[S]](
      """
        | INSERT INTO processing_tasks(id, state, owner, last_change, last_action, next_action, data)
        | VALUES (?, ?, ?, ?, now(), ?, ?)""".stripMargin
    )

  def findById(id: ProcessingTaskId): ConnectionIO[Option[ProcessingTask[S]]] = {
    sql"""
         | SELECT id, state, owner, last_change, next_action, data
         | FROM processing_tasks
         | WHERE id = $id
         | FOR UPDATE
    """.stripMargin.query[ProcessingTask[S]].option
  }

  def fetchTaskToProcess(): doobie.ConnectionIO[Option[ProcessingTask[S]]] = {
    sql"""
         | SELECT id, state, owner, last_change, next_action, data
         | FROM processing_tasks
         | WHERE next_action < now()
         | FOR UPDATE SKIP LOCKED
         | LIMIT 1
    """.stripMargin.query[ProcessingTask[S]].option
  }

  def updateLease(processingTask: ProcessingTask[S]): ConnectionIO[Unit] =
    sql"""
         | UPDATE processing_tasks SET
         | last_action = now(),
         | next_action = ${processingTask.nextAction}
         | WHERE id = ${processingTask.id}
    """.stripMargin.update.run
      .flatTap(ensureOneRowUpdated(processingTask))
      .void

  def updateOwnerAndLease(processingTask: ProcessingTask[S]): ConnectionIO[Unit] =
    sql"""
         | UPDATE processing_tasks SET
         | owner = ${processingTask.owner},
         | last_action = now(),
         | next_action = ${processingTask.nextAction}
         | WHERE id = ${processingTask.id}
    """.stripMargin.update.run
      .flatTap(ensureOneRowUpdated(processingTask))
      .void

  def update(processingTask: ProcessingTask[S]): ConnectionIO[Unit] =
    sql"""
         | UPDATE processing_tasks SET
         | state = ${processingTask.state},
         | owner = ${processingTask.owner},
         | last_change = ${processingTask.lastChange},
         | last_action = now(),
         | next_action = ${processingTask.nextAction},
         | data = ${processingTask.data}
         | WHERE id = ${processingTask.id}
    """.stripMargin.update.run
      .flatTap(ensureOneRowUpdated(processingTask))
      .void

  private def ensureOneRowUpdated(processingTask: ProcessingTask[S])(affectedRows: Int): ConnectionIO[Unit] = {
    FC.raiseError(
      new RuntimeException(s"Unknown error while updating processing task with id: ${processingTask.id} ")
    ).whenA(1 != affectedRows)
  }

  def delete(processingTaskId: ProcessingTaskId): ConnectionIO[Int] =
    sql"""
         | DELETE FROM processing_tasks
         | WHERE id = $processingTaskId
    """.stripMargin.update.run

}
