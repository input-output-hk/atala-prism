#!/bin/bash

SAMPLE_ADDRESS=`bitcoin-cli getnewaddress`
bitcoin-cli sendtoaddress $SAMPLE_ADDRESS 10.00
bitcoin-cli generatetoaddress 101 `bitcoin-cli getnewaddress`

utxo_txid=$(bitcoin-cli listunspent 1 9999999 "[\"$SAMPLE_ADDRESS\"]" | jq -r '.[0] | .txid')
utxo_vout=$(bitcoin-cli listunspent 1 9999999 "[\"$SAMPLE_ADDRESS\"]" | jq -r '.[0] | .vout')
utxo_amount=$(bitcoin-cli listunspent 1 9999999 "[\"$SAMPLE_ADDRESS\"]" | jq -r '.[0] | .amount')
changeaddress=$(bitcoin-cli getrawchangeaddress)
change=`echo "$utxo_amount - 0.0005" | bc`

op_return_data_raw="ATALA://foo"
op_return_data=$(
  hex=`echo "$op_return_data_raw" | xxd -pu `
  pad=$(printf '%0.1s' "0"{1..128})
  padlength=128
  printf '%*.*s' 0 $((padlength - ${#hex} )) "$pad"
  printf '%s\n' "$hex"
)

rawtxhex=$(bitcoin-cli -named createrawtransaction inputs='''[ { "txid": "'$utxo_txid'", "vout": '$utxo_vout' } ]''' outputs='''{ "data": "'$op_return_data'", "'$changeaddress'": '$change' }''')

signed_transaction=`bitcoin-cli signrawtransactionwithwallet $rawtxhex | jq -r '.hex'`
txid=`bitcoin-cli sendrawtransaction "$signed_transaction"`

bitcoin-cli generatetoaddress 101 `bitcoin-cli getnewaddress`
bitcoin-cli getrawtransaction "$txid" 1


