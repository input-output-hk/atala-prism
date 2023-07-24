FROM envoyproxy/envoy:v1.13.1
COPY envoy.yaml /root/envoy.yaml
COPY start_envoy.sh /usr/local/bin/start_envoy.sh
RUN apt-get update
RUN apt-get -y install gettext-base
RUN chmod u+x /usr/local/bin/start_envoy.sh
CMD ["/bin/bash", "/usr/local/bin/start_envoy.sh"]
