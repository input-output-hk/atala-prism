# End to End Encryption

End to End Encryption is a mechanism that provides security in a way that not even the service provider can decipher the data sent between participants. 
In the context of Atala PRISM it means, for example, issuers being able to send credentials to holders without us (PRISM Connector) learning its contents, including Personally Identifiable Information.


## Man in the middle attack

In cryptographic communication protocols one attack vector is third party in the middle controlling the communication between participants. 
Generally it is impossible to prevent such attacks without using some information shared between participants or another communication channel,
the attacker can communicate with participant B pretending to be A and communicating with A pretending to be B - while neither of them being able to detect it.

There exist protocols that are generally secure against MITM attack, e.g. TLS which we are using for communication between Issuer and Connector or Holder and Connector - so that part of communication should not be vulnerable.
The only missing link is the Connector itself - if the attacker controls it, they could use such power to run MITM attack.

Another potential problem, similar to MITM attack is the possibility for the attacker impersonating the holder using information known to Connector.
In such scenario, the attacker uses a connection token stolen from the Connector to instantiate a connection to the issuer and download credentials, including Personally Identifiable Information.
It is slightly different from MITM attack, as the latter involves transparently intercepting communication between parties, while described impersonation attack can be started without any action from the holder side.

Solution proposed to these issues is **treating the connection QR code as safe one-way communication channel from the Issuer** to the Holder. It can be used to share secret bits between both ends, without disclosing them to the Connector. Such shared secret can be then used to perform a handshake between the Issuer and the Holder at the beginning of connection: messages containing their public keys, signed using [JSON Web Token](https://en.wikipedia.org/wiki/JSON_Web_Token). For now such solution is not specified or implemented, it might be added later.

## Implementation

In order to ensure safe communication between an issuer and a holder in case of MITM attack, we will encrypt every message with the shared key.
For encryption we are using [AES](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard).

### Generating a key

AES key will be generated on the issuer side. AES key does not have any requirements other the size of 128, 192, or 256 bits, and for security, it should be generated
from sufficient amount of entropy. We can use EC [key derivation strategy](../protocol/key-derivation.md#generation-process) to derive EC key pair, and then use private key as AES encryption key, and public key as initialization vector for AES.
Derivation path should be calculated according to the convention for [communication keys](../protocol/key-derivation.md#the-paths-used-in-our-protocol).

In this scenario, public key is not really a "key", and neither should it be public, it is simply big chunk of bytes that can be deterministically derived from mnemonic seed, is derived from a random source and is easily associated with the encryption key it is used with.
Since AES key does not have any special requirements other than size, and EC private key is a big integer of 256 bytes, brute forcing the key would be as hard as brute forcing EC private key.
on top of that, this allows us to use one key derivation strategy do derive symmetric and asymmetric keys. Initialization vector also does not have any special requirements, other than being 128 bits long, so we can `SHA-256(pk)` and get first 128 bytes of it to use it as an Initialization vector.


### Sharing a key

An issuer will share the key the same way it shares the connection token, through QR code. Holder will obtain it by scanning the code.
A holder is responsible to store his key securely and associate it to the specific issuer, an issuer will generate different keys for every holder.

### Storing the keys

An issuer will have the database of all connections where it will associate key derivation path to [connection_id](https://github.com/input-output-hk/atala-prism-sdk/blob/003aca61418c89defad85a8b2daca97efee29a3f/protosLib/src/main/proto/connector_models.proto#L73).
This approach is preferable to storing the keys themselves, because having access to the databse only is not enough for the attacker to get access to the keys, an attacker will also need a mnemonic seed or master extended key, which is stored in the issuers wallet.


## Assumptions

* The holder scanning the QR code is a secure communication channel.
* If the issuer database is lost, the associations and keys are not restorable, every holder needs to re-connect and receive new keys.
* If the holder loses the key, he won't be able to restore it, he needs to re-connect and receive new key.

## Transition to encrypted messaging

In order to simplify the transition to encrypted messaging, we will add new type of `AtalaMessage` called
```protobuf
message EncryptedMessage {
    // Encrypted content of the message. After decryption, it should be deserialized as an AtalaMessage instance.
    bytes content = 1;
}
```
Participants will still be able to exchange unencrypted messages until we fully implement all parts of e2e encryption on all sides.

