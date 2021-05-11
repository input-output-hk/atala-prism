Instruction for running trisa testnet locally and initiating 
transactions from bob's vaps to alice's vasp. 

Despite alice and bob vasps running locally, bob's vasp still uses trisa demo directory service
for vasp address lookup. 

Those instructions were tested on Linux Ubuntu 20.04 
and with commit `updates TRIXO form sample with annotated JSON-like text (#41)` `b7f39e75` 
Some changes may be required for other operating systems.

Prerequisites:
- Clone https://github.com/trisacrypto/testnet/
- Install go: https://golang.org/doc/install
- add:
```
  export GO_PATH=/usr/local/go
  export PATH=$PATH:/$GO_PATH/bin
```

  to `~/.profile`
- Run: 
  `source ~/.profile`
- Go to trisa testnet directory
- Install venv:
  `sudo apt-get install python3-venv`
- Create venv:
  `python3 -m venv venv`
- Activate venv:
  `source venv/bin/activate`
- Generate go and javascript code from protocol buffers
  `go install ./...`
- Create fixtures certs folder: `mkdir -p fixtures/certs`
- Issue CA certificates:
  `go run ./cmd/certs init`

Running bob vasp:
- Issue bob certificates:
  `go run ./cmd/certs issue -n api.bob.vaspbot.net -O api.bob.vaspbot.net`
- Set path for certificates:
  
  `export RVASP_CERT_PATH="api.bob.vaspbot.net.gz"`
  
  `export RVASP_TRUST_CHAIN_PATH="fixtures/certs/ca.gz"`
- enable Common Name matching, otherwise you get following error:
```
"error="rpc error: code = Unavailable desc = connection error: 
desc = \"transport: authentication handshake failed: x509: 
certificate relies on legacy Common Name field, use SANs 
or temporarily enable Common Name matching with GODEBUG=x509ignoreCN=0\"
```
  
  `export GODEBUG=x509ignoreCN=0`
- Init bob db:
  `go run ./cmd/rvasp initdb -d bob.db`
- Populate bob db:
  `./scripts/rvasp.py -v bob -d bob.db`
- Run bob vasp:
  `go run ./cmd/rvasp serve -n api.bob.vaspbot.net -db bob.db -t ":4500" -a ":4501"`


Running alice vasp:

- Issue alice certificates:
  `go run ./cmd/certs issue -n api.alice.vaspbot.net -O api.alice.vaspbot.net`
- Set path for certificates:
  
  `export RVASP_CERT_PATH="api.alice.vaspbot.net.gz"`
  
  `export RVASP_TRUST_CHAIN_PATH="fixtures/certs/ca.gz"`
- Init alice db:
  `go run ./cmd/rvasp initdb -d alice.db`
- Populate alice db:
  `./scripts/rvasp.py -v alice -d alice.db`
- Run alice vasp:
  `go run ./cmd/rvasp serve -n api.alice.vaspbot.net -db alice.db`

Initiating transaction:

- Add: 
```
127.0.0.1	alice.vaspbot.net
127.0.0.1	api.alice.vaspbot.net
```
to `/etc/hosts`
This is required because bob's vasp fetches alice's vasp address from directory service. 
- Forward connections from `443` port to `4435` port:

Using iptables (tested on Ubuntu 20.04):

`sudo iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 443 -j REDIRECT --to-port 4435`

`sudo iptables -t nat -A OUTPUT -o lo -p tcp --dport 443 -j REDIRECT --to-port 4435`

Or using netcat (not working on Ubuntu 20.04 because it has different version of netcat installed by default)

`sudo nc -l -p 443 -c "nc 127.0.0.1 4435"`

This is required because bob's vasp sends transaction using https port `443` and
non root users cannot open ports below `1024`. Of course `sudo` can be used to open `443` port,
but I have problems with environment variables which cannot be easily passed. 
- Make sure that you have virtual env activated and install rvaspy:
  `pip install -e ./lib/rvaspy/`
- Create file `example.py` with following content:
```
import rvaspy

api = rvaspy.connect("localhost:4501")

cmds = [
api.account_request("robert@bobvasp.co.uk"),
api.transfer_request("robert@bobvasp.co.uk", "mary@alicevasp.us", 42.99)
]

for msg in api.stub.LiveUpdates(iter(cmds)):
print(msg)
```
- Run example.py:
  `python example.py`
  
