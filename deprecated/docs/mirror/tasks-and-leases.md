# Tasks and Leases

This document describes a system for managing tasks where instances acquire leases for processing them using a database as state storage. Its goal is to assure that each task processing is done exactly once, even in case of many instances running and some of them crashing sometimes. It can be seen as a mix of task queue, task scheduler and state machine for each task.

## Context

Atala Mirror is a component created for managing identity data in the context of cryptocurrency transactions. The goal is to make it horizontally scalable, resilient, and highly available. At the time of writing this document (May 2021) however, that is not the case: Mirror component processes messages sent to it via Atala PRISM Connector in a loop. When you run many instances, each message will get processed - and responded to - many times.

The situation is about to become even worse soon, as we are about to integrate manual Acuant flow where processing a document might take up to 24 hours. We need a way to track the progress of the verification process in a way that is not duplicated. Moreover, if an instance dies during updating the state, another one should be able to take over.

## Goals

We want to achieve a solution that is scalable, resilient, and highly available. It means that:
- many instances should be able to run at the same time with each task being processed only once,
- when one instance crashed during processing a task, it should be taken over by other instances shortly,
- all data on the state should be kept in the database so that it is not lost during crashes,
- preferably intermediate results should be stored in the database as well if possible, to avoid recomputation.

## Architecture / design / methods

The road task takes from creation to being finalized can be represented as a [finite state machine](https://en.wikipedia.org/wiki/Finite-state_machine) with some data related to the state attached (e.g. tracking id in remote service such as Acuant). Each processing happens in a specific state and might lead to transitioning into a different state. For example, user KYC flow can be in the `AWAITING_ACUANT_DOCUMENT_PROCESSING` state, waiting until documents are done being processed. Receiving the data might lead to `ACUANT_DOCUMENTS_PROCESSED` and then immediately into `ACUANT_VERIFYING_FACE_MATCH`. Each state transition should be persisted in the database so that when the instance crashes, another one can take over without repeating its steps.

Sometimes the task needs to stay in one state for a longer time, waiting for some change in  a remote system. In such case persisted information should include information on when the next check should be performed. E.g. when an instance processing task in `AWAITING_ACUANT_DOCUMENT_PROCESSING` finds that there is no update it should assure a next check is scheduled short time in the future (e.g. in 1 minute).

When one of the instances starts processing a task, it should update the database about that fact, so that no other instance starts processing it in parallel. It's going to be done by setting the id of the instance as the **owner** of the task. In such case, we'll say that instance has **lease** on the task. It is valid only for some time set by the instance. It should either clear owner information by that time or **extend the lease** by moving the end time further in the future. If the lease finishes without any update other instances should assume that the instance originally processing the task has crashed and one of them should take over.

### Database table

The table should contain the following columns:
- `id` primary key, random UUID,
- `state` state of the key, `text` in the database (or enum), represented as enum in the code,
- `owner` UUID of the instance actively processing the task (nullable); if set it means that that instance has a lease on the task until `next_action`,
- `last_change` datetime of last change of state or data,
- `last_action` last time the entry was updated (always set to `now()`),
- `next_action` time when next action should be taken,
- `data`, a `jsonb` field representing data related to the task.

### Task selection

Each instance should periodically check for tasks to be executed by querying the table for a row with `next_action < now()` (preferably ordered by `next_action`), using `SELECT FOR UPDATE` SQL query with `SKIP LOCKED` modifier. If it is not processing too many tasks in parallel, it should take a new one and process it according to the following section.

### Task processing

In such case the executing instance should:
1. Update the task, setting the `owner` column to its id and incrementing `next_action`. It should be done in one transaction in the previously mentioned `SELECT FOR UPDATE`. Column `next_action` should be set to `now() + lease_time`.
2. Do all the required processing. While the task is runnig there should be a mechanism to update periodically `next_action` column, so the lease stays active.
3. If there is any update that can be stored in the database, update `last_action`, `last_change`, and `data` fields. It can even include transitioning into another state — if it can be processed, it should be with the current lease.
4. When the task finishes or goes into the state where it needs to wait for new data, update the record clearing the `owner` column and setting the `next_action` column to the next time it should be checked. That value is going to be tasks specific, sometimes it can be 30 seconds, sometimes 30 minutes or half a day.

Before any update — lease extension or state change — the instance should execute in a transaction `SELECT FOR UPDATE`, verifying it is still the owner. If it is not, it should log warning and ignore the result. It is related to the owner ejection mechanism described in the following section.

### Owner ejection

In some states we might want to quickly modify the state, even from instance that doesn't have a lease for the task. One example here is waiting for a manual identity document verification result from Acuant which can take up to 24h. They have a feature where they can notify us about verification conclusion by REST callback. However it is not impossible for such a callback can get lost because of a bug on our or their side, leading to the user never getting a KYC credential. We should have an alternative mechanism, e.g. actively querying for the results ourselves if no callback happens for a long time.

That can, however, lead to situation when the REST callback is received by instance A when instance B is actively querying for the status and has lease over the task. In such a case, instance A should not need to wait until the lease of instance B. It should modify the task state, replacing the `owner` field with its own and taking over the task.


### Implementation details

Implementation part is less constrained, the developer has freedom to choose solutions they consider the best. The guidelines here are optional, but might be useful:
- Values from the `state` can be represented as enum.
- State from the `state` column together with the `data` can be represented as a case class implementing `Task` (or `ProcessingTask` to avoid name collision with Monix) interface.
- Task execution can be a Monix `Task` and when it is being processed, we should use Monix scheduler to periodically extend the lease if needed.
- Task selection should be a `ConnectionIO` and joined with connection claiming — that way we can assure transactionality.
- Tasks can be processed by task processors defined per task type (represented as subset of possible task sets); we can have a component routing tasks to specific processor based on their type.
- If processing of one task ends in another task that should be processed immediately, such task can be returned as a result, so that task router can choose the right processor for it.

## Alternatives considered

### Using existing Job Scheduler

There are existing scheduler implementations such as [Quartz](http://www.quartz-scheduler.org/), [Obsidian](https://web.obsidianscheduler.com/), or [Flux](https://flux.ly/) and others. Some of them support SQL-based persistence. All of them however are heavy, containing lots of features we don't need, their own SQL connection machinery. In my assessment using one of these would require more work than implementing our own based on SQL table.

### Using message broker (RabbitMQ)

Message brokers such as [RabbitMQ](https://www.rabbitmq.com/) are a great fit for task management. It is enough to create a queue, add messages there and consume messages in all instances. Scheduling tasks in the future can be done via delayed messages.

The downside of such solution is that it would require adding one more component into the deployment. That might be an overkill, especially considering that databases are perfectly fit for working as [message brokers](https://gist.github.com/chanks/7585810) or [job trackers](https://layerci.com/blog/postgres-is-the-answer/) themselves.

### Active instance / passive instance

We could avoid task processing duplication by choosing one of the instances to be active one (i.e. both processing tasks and serving GRPC) and the rest passive ones (i.e. just serving GRPC). However, such solution wouldn't be scalable horizontally and in order to achieve resilience, we would need to implement some kind of task state persistence anyway. 
