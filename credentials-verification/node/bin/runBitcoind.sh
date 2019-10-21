#!/bin/bash

function setupBitcoinStuff () {
  sleep 6
  bitcoin-cli generatetoaddress 101 `bitcoin-cli getnewaddress`
  SAMPLE_ADDRESS=`bitcoin-cli getnewaddress`
  bitcoin-cli sendtoaddress $SAMPLE_ADDRESS 10.00
  bitcoin-cli generatetoaddress 101 `bitcoin-cli getnewaddress`
}

setupBitcoinStuff &
bitcoind
