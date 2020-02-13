FROM envoyproxy/envoy:v1.12.1
COPY envoy.yaml /etc/envoy/envoy.yaml
CMD /usr/local/bin/envoy -c /etc/envoy/envoy.yaml --log-path /dev/stdout
