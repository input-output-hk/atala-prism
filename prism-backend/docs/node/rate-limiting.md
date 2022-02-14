The critical APIs shloud have proper rate-limiters to prevent a single client from consuming Node resources. `envoy` provides ways to limit requests based on sets of descriptors. 
To set up the rate-limiting, we have to do the following:
1. Take the ratelimit service implementation:
```
git clone https://github.com/envoyproxy/ratelimit
```
2. Configure rate-limiters, here's a simple example:
```
---
domain: "atala-prism"
descriptors:
  - key: remote_address
    rate_limit:
      unit: second
      requests_per_unit: 100
    descriptors:
      - key: path
        rate_limit:
          unit: second
          requests_per_unit: 20
      - key: path
        value: "/io.iohk.atala.prism.protos.NodeService/ScheduleOperations"
        rate_limit:
          unit: second
          requests_per_unit: 5
```
This config has three rate limits:
- no more than 100 requests per second from a single client
- no more than 20 requests per second from a single client with a specific gRPC call
- no more than 5 requests per second from a single client invoking `/io.iohk.atala.prism.protos.NodeService/ScheduleOperations` API call.
3. After that we can update `docker-compose.yaml` file specifying the path to the configuration, and start it using `docker-compose up`
4. In the envoy config, we need to add a new cluster with rate-limiting service:
```
  - name: rate_limit_service
    connect_timeout: 1s
    type: strict_dns
    lb_policy: round_robin
    http2_protocol_options: {} # enable H2 protocol
    load_assignment:
      cluster_name: rate_limit_service_load
      endpoints:
      - lb_endpoints:
        - endpoint:
            address:
              socket_address:
                address: localhost
                port_value: 8081
```
5. After that, we need to specify this cluster as an endpoint of the rate-limiter. To do so, we have to add this http_filter:
```
  - name: envoy.filters.http.ratelimit
    typed_config:
      "@type": type.googleapis.com/envoy.extensions.filters.http.ratelimit.v3.RateLimit
      domain: atala-prism
      rate_limit_service:
        grpc_service:
          envoy_grpc:
            cluster_name: rate_limit_service
        transport_api_version: V3
```
6. Now we can create rate limiters in the `route` section by specifying sets of descriptors:
```
rate_limits:
- actions:
  - remote_address: {}
  - request_headers:
      header_name: ":path"
      descriptor_key: path
- actions:
  - remote_address: {}
```

Here we create two sets of descriptors that will be propagated to the service:
- (remote_address, path)
- (remote_address)


