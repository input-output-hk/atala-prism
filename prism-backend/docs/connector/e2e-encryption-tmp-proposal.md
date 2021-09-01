#### There are two components that need to be implemented for connector E2E

### Key exchange protocol

For key exchange, we can use "The X3DH Key Agreement Protocol" described in details [here](https://www.signal.org/docs/specifications/x3dh/).

After the connection has been established in the connector, we will follow this protocol to exchange the common secret.

A couple of words about deriving this key in case it is lost. It will not be possible, by design. This key exchange protocol provides forward secrecy, this means that

> feature of specific key agreement protocols that gives assurances that session keys will not be compromised even if long-term secrets used in the session key exchange are compromised

from [wikipedia](https://en.wikipedia.org/wiki/Forward_secrecy)

This is achieved by deleting "one time pre-keys" which are participating in establishing a common secret after it is used once. This makes sure that even if other keys are leaked, the common secret can not be derived using them only. This all is described in details in the X3DH protocol I've linked above.

Also, as far as I'm aware, we have plans for a connector to drop messages after they've been delivered.

Based on all that, our key-derivation protocol is not very useful here because we need a way to derive one-time keys non-deterministically, and another reason is that this protocol suggests using X25519 or X448 curves, while our keys are secp256k1. [I don't really know](https://crypto.stackexchange.com/questions/93757/is-it-safe-to-implement-elliptic-curve-diffie-hellman-with-secp256k1) if secp256k1 is safe to use for this key exchange protocol. I think it would be safer and easier to use the library for X3DH that provides key generation functionality.

This protocol is designed for asynchronous key exchange scenario, so one party pushes his pre-key bundle to the server and another fetches it when he needs to establish a common secret. This adds overhead of publishing/storing keys, updating pre-key and adding new one time pre-keys periodically, but it is necessary for async key exchange. Our scenario is not really async. When a holder goes to the establishment to scan the QR code, we can start a key exchange protocol after the connection has been established. There is no need to publish pre-key bundle and manage pre-keys and one time pre-keys, the issuer will just send the bundle to the holder, and the holder will use it right away.

Since we are not using our key-derivation protocol here, I'll note that both parties will need to store their keys, including long term identity key.

I have also thought about adding those keys to a DID and letting another party resolve it and get the key this way, but this has several problems:

1. one time keys can not be added there, otherwise we need to think about updating a did (removing used key) every time it has been used, an adding new ones, once all of them have been used.
2. We can add identity key, signed pre-key and signed pre-key signature. Then the holder will request one time pre-key directly from the issuer if he needs to establish a connection. In this case we will need to periodically update signed pre-key as protocol suggests (so, we need to be updating a DID). We also have to either store private portions of this keys or figure out a way to derive them. This is harder than just sending the keys when needed, but I don't know if it is better for our use case, just sending the keys would work.

The elephant in the room with this approach is obviously the implementation. When it comes to security and cryptography, implementing things yourself is can be dangerous, and more complicated the protocol is, the more ways there are to introduce an exploit which are not present in battle tested libraries. The official signal implementation of this protocols is AGPL licensed and not really a "library", so there will be a need to introduce and maintain a custom building script that build java, swift and TS libraries and integrate them In KMM.

### Message encryption protocol

Let's assume both Issuer and Holder have derived a common secret key. Both parties are responsible for storing the keys in a secure manner, both parties should store the keys and associate it to a specific `connection_id` which will be accessible after the connection has been established. We will introduce a new type of `AtalaMessage`

```scala
message EncryptedMessage {
    // Encrypted content of the message. After decryption, it should be deserialized as an AtalaMessage instance.
    bytes content = 1;
    connection_id = 2;
}
```
`connection_id` is a unique identifier both parties will use to know which key should be used in order to decrypt an incoming message, or encrypt an outgoing one.

Cipher used for symmetric encryption is AES. There are several AES modes but from the research I've done two modes that are used the most are AES-CBC and AES-GCM. There is a good answer on [quora](https://www.quora.com/What-is-the-difference-between-AES-GCM-and-AES-CBC) and on [stackexchange](https://crypto.stackexchange.com/questions/2310/what-is-the-difference-between-cbc-and-gcm-mode) about the differences of those two

TLDR: GCM mode provides authentication mechanism on top of privacy (encryption), so it is better, and we should use that.

AES-GCM apart from the key requires an Initialization vector. Same combination of key and IV is required to decrypt the data that was encrypted with this pair, hence it is necessary that both issuer and holder to also arrive to the same IV alongside with the key.
Security requirements for IV are as follows

> An initialization vector has different security requirements than a key, so the IV usually does not need to be secret. For most block cipher modes it is important that an initialization vector is never reused under the same key, i.e. it must be a cryptographic nonce.

There is a good [answer](https://crypto.stackexchange.com/questions/84357/what-are-the-rules-for-using-aes-gcm-correctly) on stackexchange that suggests properties of IV for AEC-GCM.

specifically:

> (Key,IV) Resue: An (𝐼𝑉,𝐾𝑒𝑦) pair must never be used again

so, both issuer and holder should have new IV for every new message, and, those IVs must be the same, but they don't need to be secret.

The same stackexchange answer suggest a deterministic approach do generate an IV via Linear Feedback Shift Registers (LFSRs) or a counter, which seems reasonable to me and I suggest we usage that as well.

one possible implementation this can be as follows:

both issuer and holder need to initialize a contact based counter (so, if issuer1 is talking to holder1, he has separate counter for him, and holder1 will have a separate counter for issuer1). when issuer1 sends a message to a holder1, he generates IV by SHA-256-ing a counter and taking first 128 bits. holder1 receives a message from issuer1, increments a counter and follows the same procedure to generate an IV.

This is pretty straightforward and simple, I'm not sure if I'll go with this implementation, but it does follow the requirement of every (key,iv) combination being unique and both parties being able to derive it deterministically.

Another way would be to use some LFSR (can be [https://en.wikipedia.org/wiki/Linear-feedback_shift_register#Fibonacci_LFSRs](https://en.wikipedia.org/wiki/Linear-feedback_shift_register#Fibonacci_LFSRs)) with key as a seed value, since key is a shared common state, also happens to be a secret but that is no necessary for an IV

In case of a counter, this is a common state that needs to be in sync for both parties, if the state goes out of sync for whatever reason (system, network failure...etc) then parties won't be able to communicate, and they will have to exchange a new key and reset their counters. We might also introduce a way to synchronize a counter as one of the ways to fix the issue.