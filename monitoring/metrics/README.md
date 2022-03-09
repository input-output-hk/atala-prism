# Docs
Here you can find docs about published backend metrics.

# Connector|Management-console|Node|vault
These services use Kamon + Kanela agent for publishing metrics.
The default endpoint for metrics is `/metrics` on `9095` port.

## Basic available metrics
Following metrics by name available for collection (both in [Prometheus](http://3.141.27.100:9090/) and [Grafana](http://3.141.27.100:3000/) `admin/iohk4Ever`).

They contain basic tags such as job/instance and custom tags which are special for every custom metric.

Also, you can find basic JVM metrics in the Prometheus/Grafana. Basic JVM metrics can be turned off in the Kamon config.

- ## jvm.uptime.seconds
    Counter in seconds. Reflects uptime from the JVM start.
  
    #### Available metrics:
    1. `jvm_uptime_seconds_total` - Counter in seconds.
       Takes `kamon.metric.tick-interval` from config for incrementing value per tick.
       
    #### Available tags:
    Contains only basic tags - `ecs_cluster, ecs_task_id, ecs_task_version, instance, job, metrics_path`

- ## request-time
    Histogram with a request time in seconds.
    
    #### Available metrics:
    1. `request_time_seconds_bucket` - Buckets of [histogram](https://prometheus.io/docs/concepts/metric_types/#histogram).
    2. `request_time_seconds_count` - Total count of requests.
    3. `request_time_seconds_sum` - Sum of all requests time.

    #### Available tags:
    1. `service` - The name of a service that produces metrics can be found in the service source code.
       [This](https://github.com/input-output-hk/atala-tobearchived/blob/develop/prism-backend/connector/src/main/scala/io/iohk/atala/prism/connector/ConnectorService.scala#L50) for an example.
    2. `method` - The name of a method that produces metrics can be found in the service source code. [This](https://github.com/input-output-hk/atala-tobearchived/blob/develop/prism-backend/connector/src/main/scala/io/iohk/atala/prism/connector/ConnectorService.scala#L79) for an example.
    
- ## active-requests
    Counter of active request at the moment.
    Increments at the start of a request, decrement at the end or on the error.
  
    #### Available metrics:
    1. `active_requests` - The counter itself.

    #### Available tags:
    1. `service` - The name of a service that produces metrics can be found in the service source code.
       [This](https://github.com/input-output-hk/atala-tobearchived/blob/develop/prism-backend/connector/src/main/scala/io/iohk/atala/prism/connector/ConnectorService.scala#L50) for an example.
    2. `method` - The name of a method that produces metrics can be found in the service source code. [This](https://github.com/input-output-hk/atala-tobearchived/blob/develop/prism-backend/connector/src/main/scala/io/iohk/atala/prism/connector/ConnectorService.scala#L79) for an example.

- ## error-count
    Counter of error responses with an error code.
  
    #### Available metrics:
    1. `error_count_total` - The counter itself.

    #### Available tags:
    1. `service` - The name of a service that produces metrics can be found in the service source code.
       [This](https://github.com/input-output-hk/atala-tobearchived/blob/develop/prism-backend/connector/src/main/scala/io/iohk/atala/prism/connector/ConnectorService.scala#L50) for an example.
    2. `method` - The name of a method that produces metrics can be found in the service source code. [This](https://github.com/input-output-hk/atala-tobearchived/blob/develop/prism-backend/connector/src/main/scala/io/iohk/atala/prism/connector/ConnectorService.scala#L79) for an example.
    3. `error-code` - Response code (Only for errors). can be found [here](https://grpc.github.io/grpc/core/md_doc_statuscodes.html).
    
- ## db-query-time
    Histogram with a DB query time in seconds.
  
    #### Available metrics:
    1. `db_query_time_bucket` - Buckets of [histogram](https://prometheus.io/docs/concepts/metric_types/#histogram).
    2. `db_query_time_count` - Total count of requests.
    3. `db_query_time_sum` - Sum of all requests time.
    
    #### Available tags:
    1. `repository` - The name of a repository that produces metrics, can be found in the repository source code. [This](https://github.com/input-output-hk/atala-tobearchived/blob/develop/prism-backend/node/src/main/scala/io/iohk/atala/prism/node/repositories/CredentialBatchesRepository.scala#L62) for an example.
    2. `method` - The name of a method that produces metrics, can be found in the repository source code. [This](https://github.com/input-output-hk/atala-tobearchived/blob/develop/prism-backend/node/src/main/scala/io/iohk/atala/prism/node/repositories/CredentialBatchesRepository.scala#L63) for an example.

## Prism node specific metrics

- ## node-wallet-available-funds
    Gauge which reflects wallet available funds in `lovelace`.

  #### Available metrics:
    1. `node_wallet_available_funds` - gauge with available funds value retrieved from the wallet.

  #### Available tags:
  1. only basic tags.

- ## block-sync-status
    Two gauges with a number of the last synced block by the wallet and the next block number to sync by the prism-node.
  #### Available metrics:
  1. `node_last_synced_block_by_wallet` - gauge with the number of the last synced block by the wallet.
  2. `node_next_block_to_sync_by_prism` - gauge with the next to sync block number (by the prism-node).

    #### Available tags:
  1. only basic tags.

- ## received-atala-operations
    Counter with received atala operations.

  #### Available metrics:
    1. `received_atala_operations_total` - counter with received atala operations.

  #### Available tags:
    1. `atala_operation_type` - The name of received atala operation, can be one of 
    (`empty`, `revoke-credentials`, `create-did`, `issue-credential-batch`, `did-update`)
    2. `update_did_sub_operation` - The name of `did-update` sub-operation, can be one of 
    (`empty-did-update`, `add-key`, `remove-key`)

- ## failed-to-store-atala-objects
    Counter with failed to store atala objects.

  #### Available metrics:
    1. `failed_to_store_atala_objects_total` - counter with failed to store atala objects.

  #### Available tags:
    1. `atala_operation_type` - The name of received atala operation, can be one of
       (`empty`, `revoke-credentials`, `create-did`, `issue-credential-batch`, `did-update`)
    2. `update_did_sub_operation` - The name of `did-update` sub-operation, can be one of
       (`empty-did-update`, `add-key`, `remove-key`)
    3. `node_error_tag` - The name of the error, names can be found 
    [here](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/node/src/main/scala/io/iohk/atala/prism/node/errors/package.scala) (field - name)

- ## processed-atala-block-operations
    Counter with processed atala block operations.

  #### Available metrics:
    1. `processed_atala_block_operations_total` - counter with processed atala block operations.

  #### Available tags:
    1. `atala_operation_type` - The name of the processed atala operation, can be one of
       (`empty`, `revoke-credentials`, `create-did`, `issue-credential-batch`, `did-update`)

- ## failed-to-process-atala-block-operations
  Counter with processed atala block operations.

  #### Available metrics:
    1. `failed-to-process-atala-block-operations-total` - counter with failed to store atala objects.

  #### Available tags:
    1. `atala_operation_type` - The name of the processed atala operation, can be one of
       (`empty`, `revoke-credentials`, `create-did`, `issue-credential-batch`, `did-update`)
    2. `state_error` - The name of the process error, can be found [here](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/node/src/main/scala/io/iohk/atala/prism/node/operations/package.scala#L67) (field - name)
  
### Note: After restarting the service, all of these metrics will be reset.

# How to add metrics to the scala backend service
1. Make your module depends on common (common already have Kamon dependency).
2. Add `Kamon.init()` to your module init.
3. Add metrics into your code using utils from [prism-backend/common/src/main/scala/io/iohk/atala/prism/metrics/](https://github.com/input-output-hk/atala-tobearchived/tree/develop/prism-backend/common/src/main/scala/io/iohk/atala/prism/metrics).
   
For the full manual, you can check [Kamon official doc](https://kamon.io/docs/latest/guides/).
