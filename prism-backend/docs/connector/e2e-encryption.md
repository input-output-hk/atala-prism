# End to End Encryption

End to End Encryption is a mechanism that provides security in a way that not even the service provider can decipher the data sent between participants. In the context of Atala PRISM it means, for example, issuers being able to send credentials to holders without us (PRISM Connector) learning its contents, including Personally Identifiable Information.

## Implementation

One the of available message (`AtalaMessage`) types is `EncryptedMessage` which contains `keyId` field identifying which key has been used for encryption and `encryptedContent` which is encrypted bytes of the message itself - which is serialized `AtalaMessage` as well. `keyId` should be one of keys returned by `ConnectorService.GetConnectionCommunicationKeys`. In case of protocol participants that have DID (for now issuers and verifiers) it should be one of communication keys associated with the DID. For holders it should be the key that is provided by the mobile wallet when accepting the connection.

## Man in the middle attack

In cryptographic communication protocols one attack vector is third party in the middle controlling the communication between participants. Generally it is impossible to prevent such attacks without using some information shared between participants or another communication channel: the attacker can communicate with participand B pretending to be A and communicating with A pretending to be B - with neither of them being able to detect it.

There exist protocols that are generally secure against MITM attack, e.g. TLS which we are using for communication betwen Issuer and Connector or Holder and Connector - so that part of communication should not be vulnerable. The only missing link is the Connector itself - if the attacker controls it, they could use such power to run MITM attack.

Another potential problem, similar to MITM attack, is the possibility of the attacker impersonating the holder using information known to Connector. In such scenario, the attacker uses a connection token stolen from the Connector to instantiate a connection to the issuer and download credentials, including Personally Identifiable Information. It is slightly different from MITM attack, as the latter involves transparently intercepting communication between parties, while described impersonation attack can be started without any action from the holder side.

Solution proposed to these issues is treating the connection QR code as safe one-way communication channel from the Issuer to the Holder. It can be used to share secret bits between both ends, without disclosing them to the Connector. Such shared secret can be then used to perform a handshake between the Issuer and the Holder at the beginning of connection: messages containing their public keys, signed using [JSON Web Token](https://en.wikipedia.org/wiki/JSON_Web_Token). For now such solution is not specified or implemented, it might be added later.
