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
txindex=1
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

## Create an OP_RETURN transaction

Supposing we have a `SAMPLE_ADDRESS` with some bitcoins in it.

We can list all the unspent transaction outputs for that address with this command:
```bash
> bitcoin-cli listunspent 1 9999999 "[\"$SAMPLE_ADDRESS\"]"
[
  {
    "txid": "d0c209bfb1ca2d3aad9ac7ba7000cbe9cfd07b3da17a28cc5bf068a44009a55c",
    "vout": 1,
    "address": "2NGPcR7WuaGETYEkGYgzNmyRX2CNovLg1Jo",
    "label": "",
    "redeemScript": "00141f96f4d65ee6cb402a8415c27a5d4e26f454a3e3",
    "scriptPubKey": "a914fde0f157e8f0343209c65acfcea387ae7fb872f887",
    "amount": 10.00000000,
    "confirmations": 101,
    "spendable": true,
    "solvable": true,
    "desc": "sh(wpkh([bdd88235/0'/0'/3']02bcff851013f595d355c0b81498c48a8975458a2f63be4555d6b6b8278c41e5d0))#p2wmdzng",
    "safe": true
  }
]
```

We can then, prepare some other environment variables that are to be used to create the OP_RETURN transaction:
```bash
> utxo_txid=$(bitcoin-cli listunspent 1 9999999 "[\"$SAMPLE_ADDRESS\"]" | jq -r '.[0] | .txid')
> utxo_vout=$(bitcoin-cli listunspent 1 9999999 "[\"$SAMPLE_ADDRESS\"]" | jq -r '.[0] | .vout')
> utxo_amount=$(bitcoin-cli listunspent 1 9999999 "[\"$SAMPLE_ADDRESS\"]" | jq -r '.[0] | .amount')
> changeaddress=$(bitcoin-cli getrawchangeaddress)
> change=`echo "$utxo_amount - 0.0005" | bc`
```

Then, we will create an environment variable containing the data we want to store in the OP_RETURN:
```bash
> op_return_data_raw="foo"
> op_return_data=$(hexdump -e '"%064X"' <<< "$op_return_data_raw")
```

Next, we can get the content of the transaction we are going to send:
```bash
> rawtxhex=$(bitcoin-cli -named createrawtransaction inputs='''[ { "txid": "'$utxo_txid'", "vout": '$utxo_vout' } ]''' outputs='''{ "data": "'$op_return_data'", "'$changeaddress'": '$change' }''')
```

This is how the transaction looks like:
```bash
> bitcoin-cli -named decoderawtransaction hexstring=$rawtxhex
{
  "txid": "695f7e30ca3f3c16446d20185c105b579e4da698df9919f69810a77209dee2fe",
  "hash": "695f7e30ca3f3c16446d20185c105b579e4da698df9919f69810a77209dee2fe",
  "version": 2,
  "size": 126,
  "vsize": 126,
  "weight": 504,
  "locktime": 0,
  "vin": [
    {
      "txid": "d0c209bfb1ca2d3aad9ac7ba7000cbe9cfd07b3da17a28cc5bf068a44009a55c",
      "vout": 1,
      "scriptSig": {
        "asm": "",
        "hex": ""
      },
      "sequence": 4294967295
    }
  ],
  "vout": [
    {
      "value": 0.00000000,
      "n": 0,
      "scriptPubKey": {
        "asm": "OP_RETURN 000000000000000000000000000000000000000000000000000000000a6f6f66",
        "hex": "6a20000000000000000000000000000000000000000000000000000000000a6f6f66",
        "type": "nulldata"
      }
    },
    {
      "value": 9.99950000,
      "n": 1,
      "scriptPubKey": {
        "asm": "OP_HASH160 812153a0ee767b52aa7bc37fc1d7be1f9d0a5d72 OP_EQUAL",
        "hex": "a914812153a0ee767b52aa7bc37fc1d7be1f9d0a5d7287",
        "reqSigs": 1,
        "type": "scripthash",
        "addresses": [
          "2N5215cDb1rpYXTqvBgLW9A2CJMuB2UMFkn"
        ]
      }
    }
  ]
}
```

We can then, sign the transaction:
```bash
> signed_transaction=`bitcoin-cli signrawtransactionwithwallet $rawtxhex | jq -r '.hex'`
```

And finally, send the transaction:
```bash
> txid=`bitcoin-cli sendrawtransaction "$signed_transaction"`
```

We can now mine some blocks to move things forward:
```bash
> bitcoin-cli generatetoaddress 101 `bitcoin-cli getnewaddress`
[
  "750633052a6d213cc6df5aec6ac51395550e7673c03a244609a1339d6aebb8d3",
  [...]
  "468456e9b3250355c1434d5f75baac9484b9b879e49bc250ad5b78a236b380be"
]
```

And finally, we can check our transaction in the blockchain:
```bash
> bitcoin-cli getrawtransaction "$txid" 1
{
  "txid": "a68d5afa0b22a30311177a6fdab0deb6b523d3e31d1174519be85d7c2f38b0cb",
  "hash": "9ed7d64f45b17e87d4c61c9efa9cf2369fd62991bbb5b72210242657d2ec95c6",
  "version": 2,
  "size": 258,
  "vsize": 177,
  "weight": 705,
  "locktime": 0,
  "vin": [
    {
      "txid": "462c578f08c54fab79a7bf16a91d6122586c9dc00955aff9ae29c4d13777776e",
      "vout": 1,
      "scriptSig": {
        "asm": "0014c0942005e8eaa6ac62a674382c70acaa63dd6531",
        "hex": "160014c0942005e8eaa6ac62a674382c70acaa63dd6531"
      },
      "txinwitness": [
        "30440220108a32a0cb7079553e7f10e3990630572363ceb64002e2d1dbbac58e53406e94022015895cc794047b444ee354c94836c5195e0f75846395236e2c56ff0c08894ae801",
        "037fa4455b18b1c477d5327001744848a5ea11d206b684ac2d7edf6cfb891efd86"
      ],
      "sequence": 4294967295
    }
  ],
  "vout": [
    {
      "value": 0.00000000,
      "n": 0,
      "scriptPubKey": {
        "asm": "OP_RETURN 000000000000000000000000000000000000000000000000000000000a6f6f66",
        "hex": "6a20000000000000000000000000000000000000000000000000000000000a6f6f66",
        "type": "nulldata"
      }
    },
    {
      "value": 9.99950000,
      "n": 1,
      "scriptPubKey": {
        "asm": "OP_HASH160 ee98a500ba16a23041719faeb1bd66f6a85e5b11 OP_EQUAL",
        "hex": "a914ee98a500ba16a23041719faeb1bd66f6a85e5b1187",
        "reqSigs": 1,
        "type": "scripthash",
        "addresses": [
          "2NEzofsKJyGCdGYMFDUqtBfBgy9p8i3PoQW"
        ]
      }
    }
  ],
  "hex": "020000000001016e777737d1c429aef9af5509c09d6c5822611da916bfa779ab4fc5088f572c460100000017160014c0942005e8eaa6ac62a674382c70acaa63dd6531ffffffff020000000000000000226a20000000000000000000000000000000000000000000000000000000000a6f6f66b0069a3b0000000017a914ee98a500ba16a23041719faeb1bd66f6a85e5b1187024730440220108a32a0cb7079553e7f10e3990630572363ceb64002e2d1dbbac58e53406e94022015895cc794047b444ee354c94836c5195e0f75846395236e2c56ff0c08894ae80121037fa4455b18b1c477d5327001744848a5ea11d206b684ac2d7edf6cfb891efd8600000000",
  "blockhash": "3fdfc1b32df094b07b3cc983505794c1117000dcf141426686d2ffcbe9fe6bbf",
  "confirmations": 101,
  "time": 1569928842,
  "blocktime": 1569928842
}
```

