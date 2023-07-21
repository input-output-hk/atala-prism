# Logging

This document was created to give you some insight into backend logging.

## Tools

The preferable tool used in this project is [Tofu logging](https://docs.tofu.tf/docs/tofu.logging.home)
which have nice syntax and produces effectual log `F[Unit]`

In general, Tofu in our project produces structured and contextual logs.
That means every logged value will be reflected as a structure in the log output (we use
[ELKLayout](https://docs.tofu.tf/docs/tofu.logging.layouts#layouts) in every service)
together with `trace-id`.

`trace-id` itself is a string that will be generated on a request or will be parsed from the response header (grpc metadata)
and will be produced to the response header (grpc metadata).

The main purpose of the `trace-id` it's to give a developer some track of the request flow thru the application.


For configuration, you can use `logback.xml` (you can find it service `resources` folder, for an example
[here](../../../node/src/main/resources)). By default, every service (connector, management-console, node, vault)
use `ELKLayout` from `Tofu`, but also every service has `logback-dev.xml` setting which uses more human-readable formatting.
The main purpose of the `logback-dev.xml` is to give you more readable logs in local development when you don't have kibana etc. locally.


## Approaches

The main approach for the logs in the project it's the [Mid](https://docs.tofu.tf/docs/mid) approach.

If it's impossible to add your logs to the service in the `Mid` style - it's highly recommended considering
splitting of your service being able to use the `Mid` approach. The main idea is that if you can't use mid for logs that means
your service has too many responsibilities that will lead to more complex service which will reduce the maintainability.

Tofu uses a custom interpolator so beware that if you want to log some value you need to provide a
[loggable](https://docs.tofu.tf/docs/tofu.logging.loggable) 
instance for that value in the scope. You can use [derevo](https://github.com/tofu-tf/derevo)
(type-class derivation library) `@derive(loggable)` annotation for such purposes or define
your own
instance for the loggable type class.

Under the hood for running a request with contextual logging, we use `IOWithTraceIdContext`, a type
alias for `ReaderT[IO, TraceId, T]`. Beware if you will create logs with just `Logs.sync` or `Logging.Make.plain`
they would not contain any `trace-id` even if you created them for `IOWithTraceIdContext` as a `F[_]` since
it's not contextual logging. Always use `Logs.withContext` for creating contextual logs.
