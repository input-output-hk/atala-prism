# Deploys a prism environment composed by the node and connector:
# - There is a postgres instance for the node, and, another instance for the node.
# - The connector can talk to the node.
# - There can be any number of instances from the node and connector.
# - There must be a single postgres instance for the node, and, 1 instance for the connector.
# - Secrets are hardcoded because Vault is not integrated.
# - Envoy is used for handling the incoming traffic from external addresses.
job "job_prism" {
  # TODO: Use variable
  datacenters = ["dc1"]
  type = "service"

  group "ingress-group" {

    network {
      mode = "bridge"

      # TODO: Find a way to use a single port for all services
      port "inbound-node" {
        static = 8080
        to     = 8080
      }

      port "inbound-connector" {
        static = 8081
        to     = 8081
      }
    }

    service {
      name = "ingress-service"
      port = "8080"

      connect {
        sidecar_task {
          config {
            auth_soft_fail = true
          }

          # NOTE: Update these values to set the resources available to the main load balancer (receiving external traffic)
          resources {
            cpu    = 500
            memory = 1024
          }
        }

        gateway {

          # Consul gateway [envoy] proxy options.
          proxy {
            # The following options are automatically set by Nomad if not
            # explicitly configured when using bridge networking.
            #
            # envoy_gateway_no_default_bind = true
            # envoy_gateway_bind_addresses "uuid-api" {
            #   address = "0.0.0.0"
            #   port    = <associated listener.port>
            # }
            #
            # Additional options are documented at
            # https://www.nomadproject.io/docs/job-specification/gateway#proxy-parameters
          }

          # Consul Ingress Gateway Configuration Entry.
          ingress {
            # Nomad will automatically manage the Configuration Entry in Consul
            # given the parameters in the ingress block.
            #
            # Additional options are documented at
            # https://www.nomadproject.io/docs/job-specification/gateway#ingress-parameters
            listener {
              port     = 8080
              protocol = "tcp"
              service {
                name = "node-grpc"
              }
            }

            listener {
              port     = 8081
              protocol = "tcp"
              service {
                name = "connector-grpc"
              }
            }
          }
        }
      }
    }
  }

  group "connector-api" {
    # NOTE: Update this value to increase the number of connectors running
    count = 1

    network {
      mode = "bridge"
    }

    service {
      name = "connector-grpc"
      # when using connect this needs to be the literal value the sidecar service should proxy traffic to
      port = "50051"

      check {
          type         = "grpc"
          port         = "50051"
          interval     = "5s"
          timeout      = "2s"
          address_mode = "alloc"
          grpc_service = "connector-grpc"
        }

      # TODO: Wait for postgres/node to start before running this service
      connect {
        sidecar_task {
          config {
            auth_soft_fail = true
          }

          # NOTE: Update these values to set the resources available to the connector load balancer
          resources {
            cpu    = 500
            memory = 1024
          }
        }

        sidecar_service {
          proxy {
            upstreams {
              destination_name = "postgres-connector"
              local_bind_port  = 5432
            }

            upstreams {
              destination_name = "node-grpc"
              local_bind_port  = 50053
            }
          }
        }
      }

      # TODO: Add grpc health checks
    }

    task "connector-grpc" {
      driver = "docker"

      env {
        # TODO: Avoid hardcoding postgres details
        # TODO: Integrate Cardano
        CONNECTOR_PSQL_HOST = "${NOMAD_UPSTREAM_ADDR_postgres_connector}"
        CONNECTOR_PSQL_DATABASE = "postgres_connector"
        CONNECTOR_PSQL_USERNAME = "postgres_connector"
        CONNECTOR_PSQL_PASSWORD = "postgres_connector_pw"
        CONNECTOR_NODE_HOST = "${NOMAD_UPSTREAM_IP_node_grpc}"
        CONNECTOR_NODE_PORT = "${NOMAD_UPSTREAM_PORT_node_grpc}"
      }

      config {
        # TODO: Avoid hardcoding the url/image/version
        image = "895947072537.dkr.ecr.us-east-2.amazonaws.com/connector:develop-3652-33e5389d2"
      }

      # NOTE: Update these values to set the resources available to each connector instance
      resources {
        cpu = 500
        memory = 1024
      }
    }
  }

  group "node-api" {
    # NOTE: Update this value to increase the number of connectors running
    count = 1

    network {
      mode = "bridge"
    }

    # TODO: Wait for postgres to start before running this service
    service {
      name = "node-grpc"
      # when using connect this needs to be the literal value the sidecar service should proxy traffic to
      port = "50053"

      check {
          type         = "grpc"
          port         = "50053"
          task         = "node-grpc"
          interval     = "5s"
          timeout      = "2s"
          address_mode = "alloc"
          grpc_service = "node-grpc"
        }

      connect {
        sidecar_task {
          config {
            auth_soft_fail = true
          }

          # NOTE: Update these values to set the resources available to the node load balancer
          resources {
            cpu    = 500
            memory = 1024
          }
        }

        sidecar_service {
          proxy {
            upstreams {
              destination_name = "postgres-node"
              local_bind_port  = 5432
            }
          }
        }
      }

      # TODO: Add grpc health checks
    }

    task "node-grpc" {
      driver = "docker"

      env {
        # TODO: Avoid hardcoding the postgres details
        NODE_PSQL_HOST = "${NOMAD_UPSTREAM_ADDR_postgres_node}"
        NODE_PSQL_DATABASE = "postgres_node"
        NODE_PSQL_USERNAME = "postgres_node"
        NODE_PSQL_PASSWORD = "postgres_node_pw"
      }

      config {
        # TODO: Avoid hardcoding the url/image/version
        image = "895947072537.dkr.ecr.us-east-2.amazonaws.com/node:develop-3652-33e5389d2"
      }

      # NOTE: Update these values to set the resources available to each node instance
      resources {
        cpu = 500
        memory = 1024
      }
    }
  }

  group "node-database" {
    # NOTE: We need 1 instance, otherwise, there won't be consistency in the database
    count = 1

    network {
      mode = "bridge"
    }

    service {
      name = "postgres-node"
      # when using connect this needs to be the literal value the sidecar service should proxy traffic to
      port = "5432"

      connect {
        sidecar_service {}
      }

      # TODO: Add health checks
    }

// TODO: Enable for persistence storage
//    volume "volume_postgres_node" {
//      type      = "host"
//      read_only = false
//      # TODO: Inject from config
//      source    = "postgres_node"
//    }

    task "task_postgres_node" {
      driver = "docker"

      config {
        image = "postgres:13"
        auth_soft_fail = true
      }

      env {
        # TODO: Avoid hardcoding these values
        # There is a db created with this name
        POSTGRES_USER = "postgres_node"
        POSTGRES_PASSWORD = "postgres_node_pw"
      }

      # NOTE: Update these values to set the resources available to each postgres instance
      resources {
        cpu = 500
        memory = 1024
      }

// TODO: Enable for persistence storage
//      volume_mount {
//        # This is the volume defined above
//        volume      = "volume_postgres_node"
//        destination = "/var/lib/postgresql/data"
//        read_only   = false
//      }
    }
  }

  group "connector-database" {
    # NOTE: We need 1 instance, otherwise, there won't be consistency in the database
    count = 1

    // TODO: Enable persistence storage
    network {
      mode = "bridge"
    }

    service {
      name = "postgres-connector"
      port = "5432"

      connect {
        sidecar_service {}
      }

      # TODO: Add health checks
    }

    task "task_postgres_connector" {
      driver = "docker"

      config {
        image = "postgres:13"
        auth_soft_fail = true
      }

      env {
        # TODO: Avoid hardcoding these values
        # There is a db created with this name
        POSTGRES_USER = "postgres_connector"
        POSTGRES_PASSWORD = "postgres_connector_pw"
      }

      # NOTE: Update these values to set the resources available to each postgres instance
      resources {
        cpu = 500
        memory = 1024
      }
    }
  }
}