set -e

# Create 4 new panes (pane 0 is used by the parent process)
tmux split-window -h
tmux split-window -v
tmux select-pane -t 0
tmux split-window -v
tmux select-pane -t 0
tmux split-window -h

# Start the node
tmux select-pane -t 1 -T Node
tmux send-keys \
  "cd ../credentials-verification && mill -i node.run" \
  C-m

# Start the connector
tmux select-pane -t 2 -T Connector
tmux send-keys \
  "cd ../credentials-verification && mill -i connector.run" \
  C-m

# Start the envoy proxy
tmux select-pane -t 3 -T EnvoyProxy
tmux send-keys \
  "cd ../credentials-verification-web &&
    docker run --rm -ti --net=host -v \$PWD/envoy/envoy.yaml:/etc/envoy/envoy.yaml envoyproxy/envoy:v1.12.1 ||
    echo '
    --> It seems Envoy is already running, so nothing to do here.'" \
  C-m

# Start the browser wallet
tmux select-pane -t 4 -T BrowserWallet
tmux send-keys \
  "sbt ~chromeUnpackedFast" \
  C-m
