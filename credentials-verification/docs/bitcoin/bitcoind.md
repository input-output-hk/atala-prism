# Running a development instance of bitcoind

**NOTE:** The examples in this document have been tested using bitcoind version 0.18.1

## Setup

This documents describes how to setup a locally running bitcoin testnet, operating in 'regtest' mode.

Once you have installed bitcoind/bitcoin-cli following the needs of your system, you need to follow this steps:

1. Create a configuration file at `~/.bitcoin/bitcoin.conf`

```properties
rpcuser=bitcoin
rpcpassword=bitcoin
regtest=1
rpcport=8332
rpcallowip=127.0.0.1
rpcconnect=127.0.0.1
```

2. Start the bitcoind regtest

You have two options to start the regtest network:

a) Running it in the 'foreground'

You simply run this command:
```bash
> bitcoind
```

and the full output will appear in your terminal. You can stop it by pressing `ctrl-c`

b) Running it as a daemon (in the 'background')

Run this command to start the regtest:
```bash
> bitcoind -daemon
```

To stop the daemon:
```bash
> bitcoin-cli stop
```

3. Create some initial content in the regtest, so that the other operations can work

With the regtest running, execute the following command:

```bash
> bitcoin-cli generatetoaddress 101 `bitcoin-cli getnewaddress`
[
  "750633052a6d213cc6df5aec6ac51395550e7673c03a244609a1339d6aebb8d3",
  [...]
  "468456e9b3250355c1434d5f75baac9484b9b879e49bc250ad5b78a236b380be"
]
```

## Operations

* To create a new address:
```bash
> SAMPLE_ADDRESS=`bitcoin-cli getnewaddress`
```

* To send 10 bitcoins to the sample address:
```bash
> bitcoin-cli sendtoaddress $SAMPLE_ADDRESS 10.00
a71d95ad59a532d791eed60ca8bf844c2c0aa23427f730c6a7904041cae0d4d5
```

This bitcoins we have just sent to the sample address will be unconfirmed

* To check the balance in the address we have just created:
```bash
> bitcoin-cli getreceivedbyaddress $SAMPLE_ADDRESS
0.00000000
```

You will see it stills says that the address has no bitcoins.

We can check, instead, the unconfirmed balance we have:
```bash
> bitcoin-cli getreceivedbyaddress $SAMPLE_ADDRESS 0
10.00000000
```

To confirm this new balance, we need to mint some new blocks:
```bash
> bitcoin-cli generatetoaddress 101 `bitcoin-cli getnewaddress`
[
  "750633052a6d213cc6df5aec6ac51395550e7673c03a244609a1339d6aebb8d3",
  [...]
  "468456e9b3250355c1434d5f75baac9484b9b879e49bc250ad5b78a236b380be"
]
```

If we check the balance again, we should now have the bitcons confirmed

* To generate a single block for the new address:
```bash
> bitcoin-cli generatetoaddress 1 $SAMPLE_ADDRESS
[
  "750633052a6d213cc6df5aec6ac51395550e7673c03a244609a1339d6aebb8d3"
]
```

* To get an overview of the content of the local wallet:
```bash
> bitcoin-cli getwalletinfo
{
  "walletname": "",
  "walletversion": 169900,
  "balance": 49.99996680,
  "unconfirmed_balance": 0.00000000,
  "immature_balance": 5000.00000000,
  "txcount": 102,
  "keypoololdest": 1569434507,
  "keypoolsize": 1000,
  "keypoolsize_hd_internal": 1000,
  "paytxfee": 0.00000000,
  "hdseedid": "e621598e36410078dc98ae7d48a62e052c5f8664",
  "private_keys_enabled": true
}
```

* To see a list of addresses and what each has:
```bash
> bitcoin-cli listreceivedbyaddress
[
  {
    "address": "2NF6Uy27XhF5PXtDmQj8GGTYr89BMsC3U4D",
    "amount": 20.00000000,
    "confirmations": 101,
    "label": "",
    "txids": [
      "a71d95ad59a532d791eed60ca8bf844c2c0aa23427f730c6a7904041cae0d4d5",
      "0487ac8ee4d94aed5462c14fc273fd578ccbf4bc0d8bcd2749ef63d61bc5d7e6"
    ]
  }
]
```
