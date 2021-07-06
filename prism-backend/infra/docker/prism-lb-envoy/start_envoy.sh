#!/bin/bash
envsubst < /root/envoy.yaml > /etc/envoy/envoy.yaml && /usr/local/bin/envoy -c /etc/envoy/envoy.yaml --log-path /dev/stdout
